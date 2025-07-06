import chisel3._
import chisel3.util._
import config.Configs._

/*
tage有两类表:
1.baseTable（整体上看是一个size为2048*2way的两位饱和计数器）
    1）使用FoldedSRAMTemplate类将2048*2way折叠成512 * 8 * 2way
    2）包含一个大小为8的wrbypass，用于实现多端口的（因此wrbypass作为一个很小的cache，
    记录一定数量的表项子集，写SRAM的同时写入wrbypass；在更新时读，如果命中，就相当于读到了SRAM内的值）
    其中关于饱和计数器的更新是先读出旧值oldCtrs再根据updat_takens的情况进行更新得到新值newCtrs
2.TageTable（包含tag，tag的len同历史长度相关，以及一个三位饱和计数器ctr）
    1）每个表共4096项，2048 * 2way，分成4个bank，另有一组sram存储useful信息
    2）为每路设置一个大小为8的wrbypass，更新WrBypass时，根据是否是新分配的以及已经分配过但是WrBypass中没有，分别设ctr的值
    3）每个表的读请求包含pc、全局历史（ghist）以及已经计算好的对应长度的折叠历史（folded_hist），根据pc及表对应的历史长度的折叠历史计算出idx和tag
    4）resp包含ctr、u(usefulness域)以及unconf(用于更新useAltOnNa)

tage预测逻辑：
    s0：s0_fire时将fire、pc、folded_hist以及ghist分别发给TageTable的四个表，用于读取TageResp
    s1: 拿到tables读出的数据s1_resps(四个表的两路输出)，按路根据表序号排序并计算use_alt_on_unconf并打包到TageTableInfo，
    将(valid,tableInfo)逆序入队，其中所有valid取或得到provided表示tage有有效项，队首providerInfo表示valid=1最长历史表的输出。
    选择TageTakens：如果TageTable没有命中项或者该项use_alt_on_unconf，使用baseTable的预测结果

    计算可分配的表：s1_providers(i)表示预测块中第i条分支的provider对应的预测表序号，假设provider在预测表T2中，
    则LowerMask(UIntToOH(s1_providers(i)), TageNTables)为0b0011。 s1_provideds(i)表示预测块中第i条分支的provider是否在T1~T4，
    根据刚刚的假设，Fill(TageNTables, s1_provideds(i).asUInt)为0b1111，二者相与，得到结果为0b0011，再取反得到0b1100，
    于是可以看出T3、T4都是比provider历史长度更长的预测表。
tage更新逻辑：


*/
trait PredParams {
    def nPHT = 128
    def PHTLen = log2Ceil(nPHT)
    def nBHT = 128
    def BHTLen = log2Ceil(nBHT)
    def BHRLen = log2Ceil(nPHT)
}

class PredictorEntry extends Bundle {
    val valid = Bool()
    val brTaken = Bool()
    val pc = UInt(ADDR_WIDTH.W)
}

class BHTEntry extends Bundle with PredParams{
    val valid = Bool()
    val bhr = UInt(BHRLen.W)
}

class PHTEntry extends Bundle{
    val valid = Bool()
    val ctr = UInt(2.W)
}

class Predictor extends Module with BPUtils with PredParams{
    val io = IO(new Bundle{
        val pc_in0  = Input(UInt(ADDR_WIDTH.W))
        val pc_in1  = Input(UInt(ADDR_WIDTH.W))

        val brTaken0 = Output(Bool())
        val brTaken1 = Output(Bool())

        val update = Input(new PredictorEntry)
    })

    val bhtBank =RegInit(VecInit(Seq.fill(nBHT)(0.U.asTypeOf(new BHTEntry()))))

    val phtBank =RegInit(VecInit(Seq.fill(nPHT)(0.U.asTypeOf(new PHTEntry()))))

    val bht_idx_0, bht_idx_1, bht_idx_u = Wire(UInt(BHTLen.W))
    val pht_idx_0, pht_idx_1, pht_idx_u = Wire(UInt(PHTLen.W))

    bht_idx_0 := compute_folded_idx(io.pc_in0, ADDR_WIDTH, BHTLen)
    bht_idx_1 := compute_folded_idx(io.pc_in1, ADDR_WIDTH, BHTLen)

    val bhr0 = bhtBank(bht_idx_0).bhr
    val bhr1 = bhtBank(bht_idx_1).bhr
    
    pht_idx_0 := bhr0 ^ compute_folded_idx(io.pc_in0, ADDR_WIDTH, PHTLen)
    pht_idx_1 := bhr1 ^ compute_folded_idx(io.pc_in1, ADDR_WIDTH, PHTLen)

    io.brTaken0 := phtBank(pht_idx_0).valid && phtBank(pht_idx_0).ctr(1)
    io.brTaken1 := phtBank(pht_idx_1).valid && phtBank(pht_idx_1).ctr(1)
    
    //update
    bht_idx_u := compute_folded_idx(io.update.pc, ADDR_WIDTH, BHTLen)
    val bht_r = bhtBank(bht_idx_u).bhr
    pht_idx_u := bht_r ^ compute_folded_idx(io.update.pc, ADDR_WIDTH, PHTLen)

    when(io.update.valid){
        when(bhtBank(bht_r).valid){
            bhtBank(bht_r).bhr := Cat(bhtBank(bht_r).bhr(BHRLen - 2, 0), io.update.brTaken)
        }.otherwise{
            bhtBank(bht_r).bhr := io.update.brTaken
        }

        when(phtBank(pht_idx_u).valid){
            phtBank(pht_idx_u).ctr := satUpdate(phtBank(pht_idx_u).ctr, 2, io.update.brTaken)
        }
    }

}

