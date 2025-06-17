import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR



trait  Btb_Queue {
    val Btb_entrys = 256
    val Btb_ways = 2
    val Btb_sets = Btb_entrys / Btb_ways

    val tagsize = 20
    val brTypeNum = 2
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

trait BP_Utail extends  Btb_Queue {
    val BP_entrys = Btb_entrys //256
    val BP_ways = Btb_ways //2
    val BP_sets = Btb_sets //128

    //偏移取位
    def seg(pc:UInt,start:Int,size:Int): UInt = {
        pc(start + size - 1,start)
    }

    //len位计数器
    def BP_update(old:UInt,len:Int,Taken:Bool):UInt = {
        val least = old === 0.U
        val biggest = old === ((1 << len) - 1).U
        Mux(least && !Taken,0.U,
            Mux(biggest && Taken,((1 << len) - 1).U,
                Mux(Taken,old + 1.U,old - 1.U)))  //防止溢出
    }

    //压缩索引
    def get_idx(pc:UInt,pc_len:Int,len:Int) = {
        if(pc_len > 0){
            val nChunks = (pc_len + len - 1) / len
            val seq_Chunks = (0 until nChunks).map {i => 
                if((i + 1)*len > pc_len) {
                    pc(pc_len - 1,i * len)
                }
                else {
                    pc((i + 1) * len,i * len)
                }
            }
            Seq_XOR(seq_Chunks)
        }
        else 0.U
    }

}

class Btb_data extends Bundle with Btb_Queue{
    val valid = Bool()
    val brTaken = Bool()
    val brTarget = UInt(32.W)
    val brType = UInt(brTypeNum.W)
}

class Btb_entry extends Bundle with Btb_Queue{
    val have = Bool()
    val tag = UInt(tagsize.W)
    val data = new Btb_data()
}

class Btb_update_entry extends Bundle {
    val require = Bool()
    val update_pc = UInt(32.W) 
    val data = new Btb_data()
}

class PreInput extends Bundle {
    val bp_fire = Bool() //检测到是分支
    val br_type = UInt(32.W) //需要在译码阶段传入跳转指令的类型
    val req_pc  = UInt(32.W)
}

class My_Btb extends Module with BP_Utail{
    val io = IO(new Bundle{
        val in_0 =  Input(new PreInput)
        val in_1 =  Input(new PreInput)

        val out_0 = Output(new  Btb_data)
        val out_1 = Output(new  Btb_data)

        val update = Input(new Btb_update_entry)
    })
//-----------------------读取指令的历史记录，明白跳转的目标-------------------------------//

    val Btb_bank = RegInit(VecInit(Seq.fill(Btb_sets)
                                    (VecInit(Seq.fill(Btb_ways)
                                        (0.U.asTypeOf(new Btb_entry))))))

    val bank_idx_0 = Wire(UInt(log2Ceil(Btb_entrys).W))
    val bank_idx_1 = Wire(UInt(log2Ceil(Btb_entrys).W))

    val tag_0 = Wire(UInt(tagsize.W))
    val tag_1 = Wire(UInt(tagsize.W))

    bank_idx_0 := get_idx(io.in_0.req_pc,32,8)
    bank_idx_1 := get_idx(io.in_1.req_pc,32,8)

    tag_0 := seg(io.in_0.req_pc,2,tagsize)
    tag_1 := seg(io.in_1.req_pc,2,tagsize)

    val rEntry_0 = Wire(Vec(Btb_ways,new Btb_entry))
    val rEntry_1 = Wire(Vec(Btb_ways,new Btb_entry))

    rEntry_0 := Btb_bank(bank_idx_0)
    rEntry_1 := Btb_bank(bank_idx_1)

    val rDatas_0 = (0 until Btb_ways).map(i => rEntry_0(i).data)
    val rDatas_1 = (0 until Btb_ways).map(i => rEntry_1(i).data)

    //是跳转指令且标签一致，就是命中
    val hits_0 = (0 until Btb_ways).map(i => rEntry_0(i).tag === tag_0 && io.in_0.bp_fire && rEntry_0(i).have)
    val hits_1 = (0 until Btb_ways).map(i => rEntry_1(i).tag === tag_1 && io.in_1.bp_fire && rEntry_1(i).have)

    val hit_0 = hits_0.reduce(_ | _)
    val hit_1 = hits_1.reduce(_ | _)

/*     //返回第一个true的值对应的索引
    val hit_way0 = PriorityEncoder(hits_0)
    val hit_way1 = PriorityEncoder(hits_1)
 */

    //跳转目标
    val target_0 = PriorityMux(Seq.tabulate(Btb_ways)(i => ((hits_0(i)) -> (rDatas_0(i).brTarget))))
    val target_1 = PriorityMux(Seq.tabulate(Btb_ways)(i => ((hits_0(i)) -> (rDatas_1(i).brTarget))))

    io.out_0.brTarget := target_0
    io.out_1.brTarget := target_1

    //数据有效取决于是否命中
    io.out_0.valid := hit_0
    io.out_1.valid := hit_1

//-------------------------------更新策略-----------------------------//
    val rEntry_u =  Wire(Vec(Btb_ways,new Btb_entry))     
    val bank_idx_u = Wire(UInt(log2Ceil(Btb_entrys).W))
    val tag_u = Wire(UInt(tagsize.W))
    val hits_u =  (0 until Btb_ways).map(i => rEntry_u(i).tag === tag_u && rEntry_u(i).have)
    val lfsr = LFSR(log2Ceil(Btb_sets), io.update.require)

    bank_idx_u := get_idx(io.update.update_pc,32,idx_len)
    rEntry_u := Btb_bank(bank_idx_u) 
    tag_u := seg(io.update.update_pc,2,tagsize)

/*     val update_require_r = Reg(Bool())
    update_require_r := io.update.require  //大概需要完善 */
    //*选择路的策略：1.命中时更新数据  2.哪一个为空写入哪一个 3.随机输入一个
    val w_way = UInt(1.W)
    when(hits_u.reduce(_ || _) ){
        w_way := Mux(hits_u(0),0.U,1.U)
    }.elsewhen(!rEntry_u(0).have){
        w_way := 0.U
    }.elsewhen(!rEntry_u(1).have){
        w_way := 1.U
    }.otherwise{
        w_way := (lfsr ^ bank_idx_u).xorR  //
    }

    //有更新需求时直接更新，同时更新have位
    when(io.update.require){
        Btb_bank(bank_idx_u)(w_way).data := io.update.data
    }

    when(io.update.require && !Btb_bank(bank_idx_u)(w_way).have){
        Btb_bank(bank_idx_u)(w_way).have := !Btb_bank(bank_idx_u)(w_way).have
    }

//-----------------------------加入预测器-------------------------------//
    val predictor = Module(new my_Predictor)
    predictor.io.pc_in0 := io.in_0
    predictor.io.pc_in1 := io.in_1
    //读取数据
    io.out_0.brTaken := predictor.io.Taken_0
    io.out_1.brTaken := predictor.io.Taken_1
    //更新的接口
    predictor.io.update.valid := io.update.require
    predictor.io.update.brTaken := io.update.data.brTaken
    predictor.io.update.Pre_pc := io.update.update_pc

} 



//BHT和PHT进行预测是否该跳转
//-------------------------------预测器定义-------------------------------//
trait  Predictor_Queue {
    val nPHT = 128
    val PHTLEN = log2Ceil(nPHT)
    val nBHT = 128
    val BHTLEN = log2Ceil(nBHT)
    val BHRLEN = log2Ceil(nBHT)
}    

class Pre_Entry extends Bundle {
    val valid = Bool() //更新需求位
    val brTaken = Bool()
    val Pre_pc = UInt(32.W)
}

class BhtEntry extends Bundle with Predictor_Queue{
    val valid = Bool()
    val bhr = UInt(BHRLEN.W)
}

class PhtEntry extends Bundle {
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

    })

    val bhtBank = RegInit(VecInit(Seq.fill(nBHT)
                                        (0.U.asTypeOf(new BhtEntry))))
    val phtBank = RegInit(VecInit(Seq.fill(nPHT)
                                        (0.U.asTypeOf(new PhtEntry))))
//----------------------------读取操作---------------------------//    
    val bhtBank_idx_0 = Wire(UInt(BHTLEN.W))
    val bhtBank_idx_1 = Wire(UInt(BHTLEN.W))
    val phtBank_idx_0 = Wire(UInt(PHTLEN.W))
    val phtBank_idx_1 = Wire(UInt(PHTLEN.W))

    
    bhtBank_idx_0 := get_idx(io.pc_in0,32,BHTLEN)
    bhtBank_idx_1 := get_idx(io.pc_in1,32,BHTLEN)

    val bhr_0 = bhtBank(bhtBank_idx_0).bhr
    val bhr_1 = bhtBank(bhtBank_idx_1).bhr

    phtBank_idx_0 := get_idx(io.pc_in0,32,PHTLEN) ^ bhr_0
    phtBank_idx_1 := get_idx(io.pc_in1,32,PHTLEN) ^ bhr_1

    io.Taken_0 := phtBank(phtBank_idx_0).ctr(1) && phtBank(phtBank_idx_0).valid
    io.Taken_1 := phtBank(phtBank_idx_1).ctr(1) && phtBank(phtBank_idx_1).valid

//-----------------------------更新策略--------------------------//
    val bhtBank_idx_u = Wire(UInt(BHTLEN.W))
    val phtBank_idx_u = Wire(UInt(PHTLEN.W))
    val bhr_u = UInt(BHRLEN.W)
    val ctr_u = UInt(2.W)
    val zero = UInt(1.W)
    zero := 0.U
    bhtBank_idx_u := get_idx(io.update.Pre_pc,32,BHTLEN)
    bhr_u := bhtBank(bhtBank_idx_u).bhr
    phtBank_idx_u := bhr_u ^ bhtBank_idx_u  
    ctr_u := phtBank(phtBank_idx_u).ctr

//当有更新请求时进行更新，
//bht和pht数据更新依据是否写过，写过就正常更新，没写过就更新valid位
    when(io.update.valid){
        when(bhtBank(bhtBank_idx_u).valid){
            bhtBank(bhtBank_idx_u).bhr := Cat(bhtBank(bhtBank_idx_u).bhr(BHRLEN - 2,0),io.update.brTaken)
        }.otherwise{
            bhtBank(bhtBank_idx_u).bhr := Cat(zero,io.update.brTaken)
            bhtBank(bhtBank_idx_u).valid := !bhtBank(bhtBank_idx_u).valid
        }
        when(phtBank(phtBank_idx_u).valid){
            phtBank(phtBank_idx_u).ctr :=BP_update(ctr_u,2,io.update.brTaken)
        }.otherwise{
            phtBank(phtBank_idx_u).valid := !phtBank(phtBank_idx_u).valid
        }
    }

}



