import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR

trait  Btb_Queue {
    val Btb_entrys = 512
    val Btb_sets = 256
    val Btb_ways = 2

    val tagsize = 20
    val brTypeNum = 3
    val idx_len = 8
}

object ParallelOp {
    def apply[T <: Data](xs:Seq[T],func:(T,T) => T):T = {
        require(xs.nonEmpty)
        xs match {
            case Seq(a) => a
            case Seq(a,b) => func(a,b)
            case _ =>
                apply(Seq(apply(xs take xs.size/2, func),apply(xs drop xs.size/2,func)),func)
        }
    }
}

object Seq_XOR {
    def apply[T <: Data](xs: Seq[T]): T = {
        ParallelOp(xs, (a: Data, b: Data) => (a.asUInt ^ b.asUInt).asTypeOf(xs.head))
    }
}

object counter {
    def apply(old:UInt,width:Int,enable:Bool):UInt = {
        val max = ((1 << width) - 1).U
        Mux(enable && (old === max),max,
            Mux(enable,old + 1.U,old))
    }
}

trait BP_Utail extends  Btb_Queue {
    val BP_entrys = Btb_entrys 
    val BP_sets = Btb_sets
    val BP_ways = Btb_ways

    //偏移取位
    def seg(pc:UInt,start:Int,size:Int): UInt = {
        pc(start + size - 1,start)
    }

    //len位饱和计数器
    def BP_update(old:UInt,len:Int,Taken:Bool):UInt = {
        val least = old === 0.U
        val biggest = old === ((1 << len) - 1).U
        Mux(least && !Taken,0.U,
            Mux(biggest && Taken,((1 << len) - 1).U,
                Mux(Taken,old + 1.U,old - 1.U)))  //防止溢出
    }

    //XOR压缩索引pc -> len位
    def get_idx(pc:UInt,pc_len:Int,len:Int): UInt = {
        if(pc_len > 0){
            val nChunks = (pc_len + len - 1) / len
            val seq_Chunks = (0 until nChunks-1).map {i => 
                if((i + 1)*len > pc_len) {
                    pc(pc_len - 1,i * len)
                }
                else {
                    pc((i + 1) * len - 1,i * len)
                }
            }
            if(seq_Chunks.nonEmpty) {
                Seq_XOR(seq_Chunks)
            } else {
                pc(len-1, 0)
            }
        }
        else 0.U
    }
}

class Btb_entry extends Bundle with Btb_Queue{
    val dirty = Bool()
    val tag = UInt(tagsize.W)
    val Target = UInt(32.W)
    val Type = UInt(3.W)
}

class Btb_update_entry extends Bundle {
    val require = Bool() //同时掌管btb和predictor
    val update_pc = UInt(32.W) 
    val br_type = UInt(3.W) //需要在译码阶段传入跳转指令的类型
    val brTaken = Bool()
    val brTarget = UInt(32.W)
}

class PreInput extends Bundle {
    val bp_fire = Bool() //检测到是分支
    val req_pc  = UInt(32.W)
}

class PreOutput extends Bundle with Btb_Queue{
    val valid = Bool()
    val brTaken = Bool()
    val brType = UInt(3.W)
    val brTarget = UInt(32.W)
}

class My_Btb extends Module with BP_Utail{
    val io = IO(new Bundle{
        val in_0 =  Input(new PreInput)
        val in_1 =  Input(new PreInput)

        val out_0 = Output(new  PreOutput)
        val out_1 = Output(new  PreOutput)

        val update = Input(new Btb_update_entry)
        val counter_all = Output(UInt(32.W))
        val counter_hit = Output(UInt(32.W))
    })

    val Btb_bank = RegInit(VecInit(Seq.fill(Btb_sets)
                                    (VecInit(Seq.fill(Btb_ways)
                                        (0.U.asTypeOf(new Btb_entry))))))
        // 加入预测器
    val predictor = Module(new my_Predictor)
    predictor.io.pc_in0 := io.in_0.req_pc
    predictor.io.pc_in1 := io.in_1.req_pc
    
    //读取数据
    io.out_0.brTaken := predictor.io.Taken_0
    io.out_1.brTaken := predictor.io.Taken_1
    
    //更新的接口
    predictor.io.update.valid := io.update.require
    predictor.io.update.brTaken := io.update.brTaken
    predictor.io.update.Pre_pc := io.update.update_pc
    predictor.io.update.brType := io.update.br_type

    val bank_idx_0 = Wire(UInt(log2Ceil(Btb_sets).W))
    val bank_idx_1 = Wire(UInt(log2Ceil(Btb_sets).W))
    bank_idx_0 := (get_idx(io.in_0.req_pc, 32, idx_len) & (Btb_sets - 1).U ) 
    bank_idx_1 := (get_idx(io.in_1.req_pc, 32, idx_len) & (Btb_sets - 1).U ) 

    val tag_0 = Wire(UInt(tagsize.W))
    val tag_1 = Wire(UInt(tagsize.W))

    tag_0 := seg(io.in_0.req_pc,2,tagsize)
    tag_1 := seg(io.in_1.req_pc,2,tagsize)

    val rEntry_0 = Wire(Vec(Btb_ways,new Btb_entry))
    val rEntry_1 = Wire(Vec(Btb_ways,new Btb_entry))
    rEntry_0 := Btb_bank(bank_idx_0)
    rEntry_1 := Btb_bank(bank_idx_1)

    //将type存在btb中
    val rTargets_0 = VecInit((0 until Btb_ways).map(i => rEntry_0(i).Target))
    val rTargets_1 = VecInit((0 until Btb_ways).map(i => rEntry_1(i).Target))
    val rTypes_0 = VecInit((0 until Btb_ways).map(i => rEntry_0(i).Type))
    val rTypes_1 = VecInit((0 until Btb_ways).map(i => rEntry_1(i).Type))

    //是跳转指令且标签一致，就是命中
    val hits_0 = VecInit((0 until Btb_ways).map(i => rEntry_0(i).tag === tag_0
                                                && io.in_0.bp_fire && rEntry_0(i).dirty))
    val hits_1 = VecInit((0 until Btb_ways).map(i => rEntry_1(i).tag === tag_1 
                                                && io.in_1.bp_fire && rEntry_1(i).dirty))

    val hit_0 = hits_0.reduce(_ || _)
    val hit_1 = hits_1.reduce(_ || _)

  // ================= 计数器优化实现 =================
  // Stage1寄存输入信号，消除毛刺，按脉冲只记一次
  val fire0_r = RegNext(io.in_0.bp_fire, 0.B)
  val fire1_r = RegNext(io.in_1.bp_fire, 0.B)
  val hit0_r  = RegNext(hit_0 && fire0_r, false.B)
  val hit1_r  = RegNext(hit_1 && fire1_r, false.B)

  val counter_all = RegInit(0.U(32.W))
  val counter_hit = RegInit(0.U(32.W))
  when(reset.asBool) {
    counter_all := 0.U
    counter_hit := 0.U
  }.otherwise {
    counter_all := counter_all + fire0_r.asUInt + fire1_r.asUInt
    counter_hit := counter_hit + hit0_r.asUInt + hit1_r.asUInt
  }
  io.counter_all := counter_all
  io.counter_hit := counter_hit


    // 调试信息 - 添加条件打印，避免每个周期都打印
    when(io.in_0.bp_fire || io.in_1.bp_fire) {
        printf(p"tag_0 = ${tag_0}, io.in_0.bp_fire = ${io.in_0.bp_fire},io.in_1.bp_fire = ${io.in_1.bp_fire}\n")
        printf(p"bank_idx_0 = ${bank_idx_0}\n")
        printf(p"hit_0 = ${hit_0}, hit_1 = ${hit_1}\n")
        printf(p"counter_all = ${counter_all}, counter_hit = ${counter_hit}\n")
        for (i <- 0 until Btb_ways) {
            printf(p"Btb_bank($bank_idx_0)($i).tag = ${Btb_bank(bank_idx_0)(i).tag}, dirty = ${Btb_bank(bank_idx_0)(i).dirty}\n")
        }
    }

    //返回第一个true的值对应的索引
    val hit_way0 = PriorityEncoder(hits_0)
    val hit_way1 = PriorityEncoder(hits_1)

    val target_0 = Mux1H(hits_0, rTargets_0)
    val target_1 = Mux1H(hits_1, rTargets_1)
    val brType_0 = Mux1H(hits_0, rTypes_0)
    val brType_1 = Mux1H(hits_1, rTypes_1)
    
    val stack = Module(new Stack())
    
    val is_jalr_0 = hit_0 && (brType_0 === 3.U)
    val is_jalr_1 = hit_1 && (brType_1 === 3.U)
    val is_jalr = is_jalr_0 || is_jalr_1 
    
    stack.io.pop_en_0 := is_jalr_0
    stack.io.pop_en_1 := is_jalr_1

    val jalr_en_0 = stack.io.out_en_0
    val jalr_en_1 = stack.io.out_en_1
    val jalr_Target_0 = stack.io.out_data_0
    val jalr_Target_1 = stack.io.out_data_1

    io.out_0.brTarget := Mux(is_jalr_0 && jalr_en_0, jalr_Target_0, target_0)
    io.out_1.brTarget := Mux(is_jalr_1 && jalr_en_1, jalr_Target_1, target_1)

    io.out_0.brType := brType_0
    io.out_1.brType := brType_1

    //数据有效取决于是否命中
    io.out_0.valid := hit_0 || jalr_en_0
    io.out_1.valid := hit_1 || jalr_en_1

    // 更新策略
    val rEntry_u = Wire(Vec(Btb_ways,new Btb_entry))     
    val bank_idx_u = Wire(UInt(log2Ceil(Btb_sets).W))
    val tag_u = Wire(UInt(tagsize.W))
    val hits_u = VecInit((0 until Btb_ways).map(i => (rEntry_u(i).tag === tag_u)
                                            && rEntry_u(i).dirty))
    val lfsr = LFSR(2, io.update.require) // 修正位宽

    bank_idx_u := get_idx(io.update.update_pc,32,idx_len)
    rEntry_u := Btb_bank(bank_idx_u) 
    tag_u := seg(io.update.update_pc,2,tagsize)

    val w_way = Reg(UInt(1.W))
    when(hits_u.reduce(_ || _)) {
        w_way := Mux(hits_u(0), 0.U, 1.U)
    }.elsewhen(!rEntry_u(0).dirty) {
        w_way := 0.U
    }.elsewhen(!rEntry_u(1).dirty) {
        w_way := 1.U
    }.otherwise {
        w_way := lfsr(0)  // 使用LFSR的最低位
    }

    // 更新BTB条目
    when(io.update.require) {
        when(io.update.br_type === 2.U) {
            stack.io.push_en := true.B
            // 对于jal指令，更新tag而不是target
            Btb_bank(bank_idx_u)(w_way).tag := tag_u
            Btb_bank(bank_idx_u)(w_way).Type := io.update.br_type
            Btb_bank(bank_idx_u)(w_way).dirty := true.B
        }.otherwise {
            stack.io.push_en := false.B
            // 对于其他类型的跳转，正常更新target
            Btb_bank(bank_idx_u)(w_way).Target := io.update.brTarget
            Btb_bank(bank_idx_u)(w_way).tag := tag_u
            Btb_bank(bank_idx_u)(w_way).Type := io.update.br_type
            Btb_bank(bank_idx_u)(w_way).dirty := true.B
        }
    }.otherwise {
        stack.io.push_en := false.B
    }

    stack.io.in_data := io.update.update_pc + 4.U


}

// 预测器定义
trait  Predictor_Queue {
    val nPHT = 256
    val PHTLEN = log2Ceil(nPHT)
    val nBHT = 256
    val BHTLEN = log2Ceil(nBHT)
    val BHRLEN = 8 // 固定为8位，避免log2Ceil(nBHT)导致的问题
}    

class Pre_Entry extends Bundle {
    val valid = Bool() //更新需求位
    val brTaken = Bool()
    val brType = UInt(3.W)
    val Pre_pc = UInt(32.W)
}

class Bht_Entry extends Bundle with Predictor_Queue{
    val valid = Bool()
    val bhr = UInt(BHRLEN.W)
}

class Pht_Entry extends Bundle {
    val valid = Bool()
    val ctr = UInt(2.W)
}

class my_Predictor extends Module with Predictor_Queue with BP_Utail{
    val io = IO(new Bundle{
        val pc_in0 = Input(UInt(32.W))
        val pc_in1 = Input(UInt(32.W))
        val update = Input(new Pre_Entry)
        val Taken_0 = Output(Bool())
        val Taken_1 = Output(Bool())
        // val bhr_0 = Output(UInt(BHRLEN.W))
        // val bhr_1 = Output(UInt(BHRLEN.W))
    })

    val bhtBank = RegInit(VecInit(Seq.fill(nBHT)
                                        (0.U.asTypeOf(new Bht_Entry))))
    val phtBank = RegInit(VecInit(Seq.fill(nPHT)
                                        (0.U.asTypeOf(new Pht_Entry))))

    val bhtBank_idx_0 = Wire(UInt(BHTLEN.W))
    val bhtBank_idx_1 = Wire(UInt(BHTLEN.W))
    val phtBank_idx_0 = Wire(UInt(PHTLEN.W))
    val phtBank_idx_1 = Wire(UInt(PHTLEN.W))

    bhtBank_idx_0 := get_idx(io.pc_in0,32,BHTLEN)
    bhtBank_idx_1 := get_idx(io.pc_in1,32,BHTLEN)

    val bhr_0 = bhtBank(bhtBank_idx_0).bhr
    val bhr_1 = bhtBank(bhtBank_idx_1).bhr

    // io.bhr_0 := bhr_0
    // io.bhr_1 := bhr_1

    phtBank_idx_0 := get_idx(io.pc_in0,32,PHTLEN) ^ bhr_0
    phtBank_idx_1 := get_idx(io.pc_in1,32,PHTLEN) ^ bhr_1

    io.Taken_0 := phtBank(phtBank_idx_0).ctr(1) && phtBank(phtBank_idx_0).valid
    io.Taken_1 := phtBank(phtBank_idx_1).ctr(1) && phtBank(phtBank_idx_1).valid

    // 更新策略
    val bhtBank_idx_u = Wire(UInt(BHTLEN.W))
    val phtBank_idx_u = Wire(UInt(PHTLEN.W))
    val bhr_u = Wire(UInt(BHRLEN.W))
    val ctr_u = Wire(UInt(2.W))
    
    bhtBank_idx_u := get_idx(io.update.Pre_pc,32,BHTLEN)
    bhr_u := bhtBank(bhtBank_idx_u).bhr
    phtBank_idx_u := bhr_u ^ get_idx(io.update.Pre_pc,32,PHTLEN)  // 修正这里
    ctr_u := phtBank(phtBank_idx_u).ctr

    when(io.update.valid){
        when(bhtBank(bhtBank_idx_u).valid){
            bhtBank(bhtBank_idx_u).bhr := Cat(bhtBank(bhtBank_idx_u).bhr(BHRLEN - 2,0),io.update.brTaken)
        }.otherwise{
            bhtBank(bhtBank_idx_u).bhr := Cat(0.U((BHRLEN-1).W),io.update.brTaken) 
            bhtBank(bhtBank_idx_u).valid := true.B
        }

        when(phtBank(phtBank_idx_u).valid){
            phtBank(phtBank_idx_u).ctr := BP_update(ctr_u,2,io.update.brTaken)
        }.otherwise{
            phtBank(phtBank_idx_u).ctr := BP_update(0.U,2,io.update.brTaken)
            phtBank(phtBank_idx_u).valid := true.B
        }
    }
}

// 栈定义
class Stack extends Module{
    val io = IO(new Bundle{
        val push_en  = Input(Bool())
        val pop_en_0 = Input(Bool())
        val pop_en_1 = Input(Bool())
        val in_data  = Input(UInt(32.W))
        val out_en_0 = Output(Bool())
        val out_en_1 = Output(Bool())
        val out_data_0 = Output(UInt(32.W))
        val out_data_1 = Output(UInt(32.W))
    })
    
    val ptr = RegInit(0.U(3.W))
    val zero_have = RegInit(false.B)
    val stack_bank = RegInit(VecInit(Seq.fill(8)(0.U(32.W))))
    
    val empty_0 = ptr === 0.U && !zero_have
    val empty_1 = ptr === 1.U && !zero_have
    val full = ptr === 7.U && io.push_en
    
    val pop_one = (io.pop_en_0 && !io.pop_en_1) || (!io.pop_en_0 && io.pop_en_1)
    val pop_two = io.pop_en_0 && io.pop_en_1
    val both = (pop_one || pop_two) && io.push_en
    
    // 简化逻辑
    when(io.push_en && !full && !both){
        stack_bank(ptr) := io.in_data
        when(ptr === 0.U) {
            zero_have := true.B
        }.otherwise {
            ptr := ptr + 1.U
        }
    }.elsewhen(pop_one && !empty_0 && !both){
        when(ptr === 0.U && zero_have) {
            zero_have := false.B
        }.otherwise {
            ptr := ptr - 1.U
        }
    }.elsewhen(pop_two && !empty_0 && !empty_1 && !both){
        when(ptr >= 2.U) {
            ptr := ptr - 2.U
        }.elsewhen(ptr === 1.U && zero_have) {
            ptr := 0.U
            zero_have := false.B
        }
    }
    
    // 输出逻辑
    io.out_data_0 := Mux(io.pop_en_0 && !empty_0, 
                        Mux(ptr === 0.U && zero_have, stack_bank(0), stack_bank(ptr - 1.U)), 
                        0.U)
    io.out_data_1 := Mux(io.pop_en_1 && !empty_1, 
                        Mux(pop_two && ptr >= 2.U, stack_bank(ptr - 2.U), stack_bank(ptr - 1.U)), 
                        0.U)
    
    io.out_en_0 := io.out_data_0 =/= 0.U
    io.out_en_1 := io.out_data_1 =/= 0.U
}