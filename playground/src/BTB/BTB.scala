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
    // BHT：分支历史表，存储局部历史
    val nBHT = 128
    val BHTLen = log2Ceil(nBHT)
    val BHRLen = 6  // 6位历史长度
    val bhtBank = RegInit(VecInit(Seq.fill(nBHT)(0.U(BHRLen.W))))

    // PHT：模式历史表，存储2位饱和计数器
    val nPHT = 128
    val PHTLen = log2Ceil(nPHT)
    val phtBank = RegInit(VecInit(Seq.fill(nPHT)(1.U(2.W)))) // 初始为弱taken

    // LRU管理器
    val lruManager = Module(new LRUManager(nSets))

    // 命中/访问计数器
    val hit_count_0_reg = RegInit(0.U(32.W))
    val hit_count_1_reg = RegInit(0.U(32.W))
    val access_count_0_reg = RegInit(0.U(32.W))
    val access_count_1_reg = RegInit(0.U(32.W))

    // 端口0处理
    val idx_0 = get_seg(io.in_0.req_pc, log2Ceil(nSets), 2)
    val tag_0 = get_seg(io.in_0.req_pc, tagSize, 2 + log2Ceil(nSets))
    
    val rdata_0 = btbBank(idx_0)
    val hits_0 = VecInit((0 until nWays).map(i => 
        rdata_0(i).valid && rdata_0(i).tag === tag_0 && io.in_0.bp_fire))
    val hit_0 = hits_0.reduce(_||_)
    val hit_way_0 = OHToUInt(hits_0)
    val resp_0 = Mux1H(hits_0, rdata_0.map(_.entry))

    // 端口0的两级预测
    val bht_idx_0 = get_seg(io.in_0.req_pc, BHTLen, 2)
    val bhr_0 = bhtBank(bht_idx_0)
    val pht_idx_0 = bhr_0 ^ get_seg(io.in_0.req_pc, PHTLen, 2)
    val pred_taken_0 = phtBank(pht_idx_0)(1)

    // 端口1处理
    val idx_1 = get_seg(io.in_1.req_pc, log2Ceil(nSets), 2)
    val tag_1 = get_seg(io.in_1.req_pc, tagSize, 2 + log2Ceil(nSets))
    
    val rdata_1 = btbBank(idx_1)
    val hits_1 = VecInit((0 until nWays).map(i => 
        rdata_1(i).valid && rdata_1(i).tag === tag_1 && io.in_1.bp_fire))
    val hit_1 = hits_1.reduce(_||_)
    val hit_way_1 = OHToUInt(hits_1)
    val resp_1 = Mux1H(hits_1, rdata_1.map(_.entry))

    // 端口1的两级预测
    val bht_idx_1 = get_seg(io.in_1.req_pc, BHTLen, 2)
    val bhr_1 = bhtBank(bht_idx_1)
    val pht_idx_1 = bhr_1 ^ get_seg(io.in_1.req_pc, PHTLen, 2)
    val pred_taken_1 = phtBank(pht_idx_1)(1)

    // 输出
    io.out_0.brTaken := hit_0 && pred_taken_0
    io.out_0.entry := resp_0
    io.out_1.brTaken := hit_1 && pred_taken_1
    io.out_1.entry := resp_1

    // LRU查询
    lruManager.io.access_0.valid := io.in_0.bp_fire
    lruManager.io.access_0.bits := idx_0
    lruManager.io.access_1.valid := io.in_1.bp_fire
    lruManager.io.access_1.bits := idx_1

    // 命中/访问计数
    when(io.in_0.bp_fire) {
        access_count_0_reg := access_count_0_reg + 1.U
        when(hit_0) {
            hit_count_0_reg := hit_count_0_reg + 1.U
        }
    }
    when(io.in_1.bp_fire) {
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