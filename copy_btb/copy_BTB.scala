import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR
import javax.xml.crypto.Data


trait  Btb_Queue {
    val Btb_entrys = 256
    val Btb_ways = 2
    val Btb_sets = Btb_entrys / Btb_ways

    val tagsize = 20
    val brTypeNum = 2
    val idx_len = 8
}

object ParallelOp {
    def apply[T](xs:Seq[T],func:(T,T) => T):T = {
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
    def apply[T <: Data](xs: Seq[T]):T = {
        ParallelOp(xs,(a:T,b:T) => (a.asUInt ^ b.asUInt).asTypeOf(xs.head))
    }
}

trait BP_Utail extends  Btb_Queue {
    val BP_entrys = Btb_entrys //256
    val BP_ways = Btb_ways //2
    val BP_sets = Btb_sets //128

    //偏移取位
    def seg(pc:UInt,start:UInt,size:Int):UInt = {
        pc(start + size - 1,start)
    }

    //len位计数器

    def BP_update(old:UInt,len:UInt,Taken:Bool):UInt = {
        val least = old === 0.U
        val biggest = old === ((1<<len) - 1).U
        Mux(least && ~Taken,0.U,
            Mux(biggest && Taken,((1<<len) - 1).U)
                Mux(Taken,old + 1.U,old - 1.U))  //防止溢出
    }

    //压缩索引
    def get_idx(pc:UInt,pc_len:UInt,len:UInt):UInt = {
        if(pc_len > 0.U){
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
    val brTaken = Bool()
    val brTarget = UInt(32.W)
    val brType = UInt(brTypeNum.W)
}

class Btb_entry extends Bundle with Btb_data with Btb_Queue{
    val valid = Bool()
    val tag = UInt(tagsize.W)
    val data = new Btb_data()
}

class PredictorInput extends Bundle with Btb_data with Btb_entry{
    val bp_fire = Bool() //检测到是分支
    val req_pc  = UInt(32.W)
}

class Btb extends Module with BP_Utail{
    val io = IO(new Bundle{
        val in_0 =  Input(new PredictorInput)
        val in_1 =  Input(new PredictorInput)

        val out_0 = Output(new  Btb_data)
        val out_1 = Output(new  Btb_data)

        val updata = Input(new Btb_data)
    })
   
    val Btb_bank = RegInit(VecInit(Seq.fill(Btb_sets)
                                    (VecInit(Seq.fill(Btb_ways)
                                        (0.U.asTypeOf(new Btb_entry))))))

    val bank_idx_0 = Wire(UInt(log2Ceil(Btb_entrys).W))
    val bank_idx_1 = Wire(UInt(log2Ceil(Btb_entrys).W))
    val bank_idx_u = Wire(UInt(log2Ceil(Btb_entrys).W))

    val tag_0 = Wire(UInt(tagsize.W))
    val tag_1 = Wire(UInt(tagsize.W))
    val tag_u = Wire(UInt(tagsize.W))
    
    bank_idx_0 := get_idx(io.in_0.req_pc,32.U,8.U)
    bank_idx_1 := get_idx(io.in_1.req_pc,32.U,8.U)

    tag_0 := seg(io.in_0.req_pc,2,tagsize)
    tag_1 := seg(io.in_1.req_pc,2,tagsize)

    val rEntry_0 = Wire(Vec(Btb_ways,new Btb_entry))
    val rEntry_1 = Wire(Vec(Btb_ways,new Btb_entry))

    rEntry_0 := Btb_bank(bank_idx_0)
    rEntry_1 := Btb_bank(bank_idx_1)

    val rDatas_0 = (0.U until Btb_ways).map(i => rEntry_0(i).data)
    val rDatas_1 = (0.U until Btb_ways).map(i => rEntry_1(i).data)

    val hits_0 = (0.U until Btb_ways).map(i => rEntry_0(i).tag === tag_0 && io.in_0.bp_fire)
    val hits_1 = (0.U until Btb_ways).map(i => rEntry_1(i).tag === tag_1 && io.in_1.bp_fire)
    //是跳转指令且标签一致

























} 





