import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR
import config.Configs._
import config.BtbParams._

trait BPUtils {
    val nEntries = BTB_Entrys
    val nWays    = BTB_Ways
    val nSets    = nEntries/nWays
    val tagSize  = 20

    def satUpdate(old: UInt, len: Int, taken: Bool): UInt = {
        val oldSatTaken = old === ((1 << len)-1).U
        val oldSatNotTaken = old === 0.U
        Mux(oldSatTaken && taken, ((1 << len)-1).U,
        Mux(oldSatNotTaken && !taken, 0.U,
            Mux(taken, old + 1.U, old - 1.U)))
    }

    def get_seg(pc: UInt, size: Int, star: Int): UInt = {
        pc(star + size - 1, star)
    }

    def compute_folded_idx(hist: UInt, histLen: Int, l: Int): UInt = {
        if (histLen > 0) {
            val nChunks = (histLen + l - 1) / l
            val hist_chunks = (0 until nChunks) map {i =>
                if( (i+1)*l > histLen){
                    hist(histLen-1, i*l)
                }else{
                    hist((i+1)*l-1, i*l)
                }
            }
            ParallelXOR(hist_chunks)
        }
        else 0.U
    }
}

class BTBEntry extends Bundle {
    val brType   = UInt(BrTypeWidth.W)
    val brTarget = UInt(ADDR_WIDTH.W)
}

class BTBEntryWithTag extends Bundle with BPUtils{
    val valid = Bool()
    val tag = UInt(tagSize.W)
    val entry = new BTBEntry()
}

class PredictorInput extends Bundle {
    val bp_fire = Bool()
    val req_pc  = UInt(ADDR_WIDTH.W)
}

class PredictorOutput extends Bundle {
    val brTaken = Bool()
    val entry   = new BTBEntry()
}

// RAS模块，支持端口0 push/pop，且只有PC变化时才操作
class RAS(val depth: Int = 16) extends Module {
    val io = IO(new Bundle {
        val push = Input(Bool())
        val pop = Input(Bool())
        val addr_in = Input(UInt(ADDR_WIDTH.W))
        val addr_out = Output(UInt(ADDR_WIDTH.W))
        val pc = Input(UInt(ADDR_WIDTH.W))
        val pop_pc = Input(UInt(ADDR_WIDTH.W))
        val push_pc = Input(UInt(ADDR_WIDTH.W))
    })

    val stack = RegInit(VecInit(Seq.fill(depth)(0.U(ADDR_WIDTH.W))))
    val sp = RegInit(0.U(log2Ceil(depth).W))

    // 上次操作PC记录
    val last_push_pc = RegInit(0.U(ADDR_WIDTH.W))
    val last_pop_pc = RegInit(0.U(ADDR_WIDTH.W))

    // 只有PC变化时才真正push/pop
    val push_en = io.push && (io.push_pc =/= last_push_pc)
    val pop_en = io.pop && (io.pop_pc =/= last_pop_pc)

    io.addr_out := stack(Mux(sp === 0.U, 0.U, sp - 1.U))

    when(push_en) {
        stack(sp) := io.addr_in
        when(sp =/= (depth-1).U) { sp := sp + 1.U }
        last_push_pc := io.push_pc
    }
    when(pop_en) {
        when(sp =/= 0.U) { sp := sp - 1.U }
        last_pop_pc := io.pop_pc
    }
}

// LRU管理器，用于2路组相联的LRU替换
class LRUManager(nSets: Int) extends Module {
    val io = IO(new Bundle {
        val access_0 = Input(Valid(UInt(log2Ceil(nSets).W)))
        val access_1 = Input(Valid(UInt(log2Ceil(nSets).W)))
        val update   = Input(Valid(UInt(log2Ceil(nSets).W)))
        val lru_way_0 = Output(UInt(1.W))
        val lru_way_1 = Output(UInt(1.W))
        val lru_way_u = Output(UInt(1.W))
    })

    // 对于2路组相联，每组只需要1bit来表示LRU
    // 0表示路0是LRU，1表示路1是LRU
    val lru_bits = RegInit(VecInit(Seq.fill(nSets)(false.B)))

    // 查询LRU路
    io.lru_way_0 := Mux(lru_bits(io.access_0.bits), 0.U, 1.U)
    io.lru_way_1 := Mux(lru_bits(io.access_1.bits), 0.U, 1.U)
    io.lru_way_u := Mux(lru_bits(io.update.bits), 0.U, 1.U)

    // 更新LRU状态
    when(io.update.valid) {
        lru_bits(io.update.bits) := ~lru_bits(io.update.bits)
    }
}

class BTB extends Module with BPUtils {
    val io = IO(new Bundle{
        val in_0  = Input(new PredictorInput)
        val in_1  = Input(new PredictorInput)

        val out_0 = Output(new PredictorOutput)
        val out_1 = Output(new PredictorOutput)

        val update = Input(new PredictorUpdate)
        
        // 命中/访问计数器
        val hit_count_0 = Output(UInt(32.W))
        val hit_count_1 = Output(UInt(32.W))
        val access_count_0 = Output(UInt(32.W))
        val access_count_1 = Output(UInt(32.W))
    })

    // BTB存储体：2路组相联
    val btbBank = RegInit(VecInit(Seq.fill(nSets)(VecInit(Seq.fill(nWays)(0.U.asTypeOf(new BTBEntryWithTag()))))))

    // 集成的两级分支预测器
    val nBHT = 128
    val BHTLen = log2Ceil(nBHT)
    val BHRLen = 6 //历史寄存器长度
    val bhtBank = RegInit(VecInit(Seq.fill(nBHT)(0.U(BHRLen.W))))

    // PHT：模式历史表，存储2位饱和计数器
    val nPHT = 128
    val PHTLen = log2Ceil(nPHT)
    val phtBank = RegInit(VecInit(Seq.fill(nPHT)(1.U(2.W)))) // 初始化为弱跳转    

    // LRU管理器
    val lruManager = Module(new LRUManager(nSets))

    // 命中/访问计数器
    val hit_count_0_reg = RegInit(0.U(32.W))
    val hit_count_1_reg = RegInit(0.U(32.W))
    val access_count_0_reg = RegInit(0.U(32.W))
    val access_count_1_reg = RegInit(0.U(32.W))

    // 端口0/1上次PC记录
    val last_pc_0 = RegInit(0.U(ADDR_WIDTH.W))
    val last_pc_1 = RegInit(0.U(ADDR_WIDTH.W))
    val pc_0_changed = io.in_0.req_pc =/= last_pc_0
    val pc_1_changed = io.in_1.req_pc =/= last_pc_1
    val real_valid_0 = io.in_0.bp_fire && pc_0_changed
    val real_valid_1 = io.in_1.bp_fire && pc_1_changed

    when(real_valid_0) { last_pc_0 := io.in_0.req_pc }
    when(real_valid_1) { last_pc_1 := io.in_1.req_pc }

    // RAS实例
    val ras = Module(new RAS(16))
    dontTouch(ras.stack) // 保持RAS栈的可见性
    dontTouch(ras.sp) // 保持RAS栈指针的可见性
    

    // 端口0 PHT读取
    val bht_idx_0 = get_seg(io.in_0.req_pc, BHTLen, 2)
    val bhr_0 = bhtBank(bht_idx_0)
    val pht_idx_0 = bhr_0 ^ get_seg(io.in_0.req_pc, PHTLen, 2)
    val pht_ctr_0 = phtBank(pht_idx_0)

    // 端口1 PHT读取
    val bht_idx_1 = get_seg(io.in_1.req_pc, BHTLen, 2)
    val bhr_1 = bhtBank(bht_idx_1)
    val pht_idx_1 = bhr_1 ^ get_seg(io.in_1.req_pc, PHTLen, 2)
    val pht_ctr_1 = phtBank(pht_idx_1)

    // 端口0处理
    val idx_0 = get_seg(io.in_0.req_pc, log2Ceil(nSets), 2)
    val tag_0 = get_seg(io.in_0.req_pc, tagSize, 2 + log2Ceil(nSets))
    val rdata_0 = btbBank(idx_0)
    val hits_0 = VecInit((0 until nWays).map(i => 
        rdata_0(i).valid && rdata_0(i).tag === tag_0 && real_valid_0))
    val hit_0 = hits_0.reduce(_||_)
    val hit_way_0 = OHToUInt(hits_0)
    val resp_0 = Mux1H(hits_0, rdata_0.map(_.entry))

    // 端口1处理
    val idx_1 = get_seg(io.in_1.req_pc, log2Ceil(nSets), 2)
    val tag_1 = get_seg(io.in_1.req_pc, tagSize, 2 + log2Ceil(nSets))
    val rdata_1 = btbBank(idx_1)
    val hits_1 = VecInit((0 until nWays).map(i => 
        rdata_1(i).valid && rdata_1(i).tag === tag_1 && real_valid_1))
    val hit_1 = hits_1.reduce(_||_)
    val hit_way_1 = OHToUInt(hits_1)
    val resp_1 = Mux1H(hits_1, rdata_1.map(_.entry))

    // 跳转预测策略
    def predict_taken(brType: UInt, pht_ctr: UInt): Bool = {
        val taken = WireDefault(false.B)
        when(brType === "b0001".U) { // beq/bne
            taken := pht_ctr(1)
        }.elsewhen(brType === "b0010".U) { // BL
            taken := true.B
        }.elsewhen(brType === "b0100".U) { // B (ret)
            taken := true.B
        }.elsewhen(brType === "b1000".U) { // jirl
            taken := false.B
        }
        taken
    }

    // RAS操作信号（只在PC变化时push/pop）
    val ras_push_0 = hit_0 && (resp_0.brType(1) || resp_0.brType(3))
    val ras_pop_0  = hit_0 && (resp_0.brType(2))
    ras.io.push := ras_push_0
    ras.io.pop  := ras_pop_0
    ras.io.addr_in := io.in_0.req_pc + 4.U
    ras.io.pc := io.in_0.req_pc
    ras.io.push_pc := io.in_0.req_pc
    ras.io.pop_pc := io.in_0.req_pc

    // 端口0输出
    val out0_brTaken = hit_0 && predict_taken(resp_0.brType, pht_ctr_0)
    val out0_entry = Wire(new BTBEntry)
    out0_entry := resp_0
    when(hit_0 && resp_0.brType(2)) { // ret
        out0_entry.brTarget := ras.io.addr_out
    }
    io.out_0.brTaken := out0_brTaken
    io.out_0.entry := out0_entry

    // 端口1输出
    val out1_brTaken = hit_1 && predict_taken(resp_1.brType, pht_ctr_1)
    val out1_entry = Wire(new BTBEntry)
    out1_entry := resp_1
    when(hit_1 && resp_1.brType(2)) {
        out1_entry.brTarget := ras.io.addr_out
    }

    io.out_1.brTaken := out1_brTaken
    io.out_1.entry := out1_entry

    // LRU查询
    lruManager.io.access_0.valid := real_valid_0
    lruManager.io.access_0.bits := idx_0
    lruManager.io.access_1.valid := real_valid_1
    lruManager.io.access_1.bits := idx_1

    // 命中/访问计数
    when(real_valid_0) {
        access_count_0_reg := access_count_0_reg + 1.U
        when(hit_0) {
            hit_count_0_reg := hit_count_0_reg + 1.U
        }
    }
    when(real_valid_1) {
        access_count_1_reg := access_count_1_reg + 1.U
        when(hit_1) {
            hit_count_1_reg := hit_count_1_reg + 1.U
        }
    }

    io.hit_count_0 := hit_count_0_reg
    io.hit_count_1 := hit_count_1_reg
    io.access_count_0 := access_count_0_reg
    io.access_count_1 := access_count_1_reg

    // 更新逻辑
    val idx_u = get_seg(io.update.pc, log2Ceil(nSets), 2)
    val tag_u = get_seg(io.update.pc, tagSize, 2 + log2Ceil(nSets))
    val rdata_u = btbBank(idx_u)
    val hits_u = VecInit((0 until nWays).map(i => 
        rdata_u(i).valid && rdata_u(i).tag === tag_u))
    val hit_u = hits_u.reduce(_||_)
    val hit_way_u = OHToUInt(hits_u)

    // LRU更新
    lruManager.io.update.valid := io.update.valid
    lruManager.io.update.bits := idx_u

    // 写入路选择：命中则更新，否则检查空路，最后LRU替换
    val has_invalid_u = rdata_u.map(!_.valid).reduce(_||_)
    val invalid_way_u = PriorityEncoder(rdata_u.map(!_.valid))
    val lru_way_u = lruManager.io.lru_way_u

    val write_way = Wire(UInt(1.W))
    write_way := Mux(hit_u, hit_way_u,
                 Mux(has_invalid_u, invalid_way_u, lru_way_u))

    // BTB更新
    val update_data = Wire(new BTBEntryWithTag)
    update_data.valid := true.B
    update_data.tag := tag_u
    update_data.entry := io.update.entry

    when(io.update.valid) {
        btbBank(idx_u)(write_way) := update_data
    }

    // 两级预测器更新
    val bht_idx_u = get_seg(io.update.pc, BHTLen, 2)
    val bhr_u = bhtBank(bht_idx_u)
    val pht_idx_u = bhr_u ^ get_seg(io.update.pc, PHTLen, 2)

    when(io.update.valid) {
        // 更新BHT
        bhtBank(bht_idx_u) := Cat(bhr_u(BHRLen-2, 0), io.update.brTaken)
        // 更新PHT
        phtBank(pht_idx_u) := satUpdate(phtBank(pht_idx_u), 2, io.update.brTaken)
    }
}