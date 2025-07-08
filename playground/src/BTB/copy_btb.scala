import chisel3._
import chisel3.util._


trait  Btb_Queue {
    val Btb_entrys = 512
    val Btb_ways = 2
    val Btb_sets = Btb_entrys / Btb_ways
    val tagsize = 20
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
    def get_idx(pc: UInt, pc_len: Int, len: Int): UInt = {
        // 去除对齐位，使用PC[31:2]参与计算
        val effective_pc = pc >> 2
        val effective_pc_len = pc_len - 2
        
        val nChunks = (effective_pc_len + len - 1) / len
        val seq_Chunks = (0 until nChunks).map { i =>
            val high = math.min(effective_pc_len - 1, (i + 1) * len - 1)
            val low  = i * len
            effective_pc(high, low)
        }
        
        // 额外的高位混合，减少局部密集冲突
        val folded_result = Seq_XOR(seq_Chunks)
        val high_bits = effective_pc(effective_pc_len - 1, effective_pc_len - len)
        
        // 最终哈希：折叠结果 XOR 高位
        folded_result ^ high_bits
    }
}

class Btb_entry extends Bundle with Btb_Queue{
    val dirty = Bool()
    val tag = UInt(tagsize.W)
    val Target = UInt(32.W)
    val Type = UInt(3.W)
    // 优化2: 添加LRU位支持更好的替换策略
    val lru_counter = UInt(2.W)
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
    dontTouch(Btb_bank)
//---------------------------加入预测器---------------------------//
    val predictor = Module(new my_Predictor)
    predictor.io.pc_in0 := io.in_0.req_pc
    predictor.io.pc_in1 := io.in_1.req_pc
    
    //读取数据
    io.out_0.brTaken := predictor.io.Taken_0
    io.out_1.brTaken := predictor.io.Taken_1
    
    //更新的接口
    //只有条件跳转需要预测taken
    predictor.io.update.valid := io.update.require && (io.update.brTarget === 1.U)
    predictor.io.update.brTaken := io.update.brTaken
    predictor.io.update.Pre_pc := io.update.update_pc
    predictor.io.update.brType := io.update.br_type

//---------------------------读取BTB---------------------------//
    val bank_idx_0 = Wire(UInt(log2Ceil(Btb_sets).W))
    val bank_idx_1 = Wire(UInt(log2Ceil(Btb_sets).W))
    val tag_0 = Wire(UInt(tagsize.W))
    val tag_1 = Wire(UInt(tagsize.W))
    val rSet_0 = Wire(Vec(Btb_ways,new Btb_entry))
    val rSet_1 = Wire(Vec(Btb_ways,new Btb_entry))

    bank_idx_0 := (get_idx(io.in_0.req_pc, 32, idx_len) & (Btb_sets - 1).U ) 
    bank_idx_1 := (get_idx(io.in_1.req_pc, 32, idx_len) & (Btb_sets - 1).U ) 
    tag_0 := io.in_0.req_pc(31, 32-tagsize)
    tag_1 := io.in_1.req_pc(31, 32-tagsize)
    rSet_0 := Btb_bank(bank_idx_0)
    rSet_1 := Btb_bank(bank_idx_1)

    //是跳转指令且标签一致，就是命中
    //todo 考虑是否需要未堵塞信号bp_fire
    val hits_0 = VecInit((0 until Btb_ways).map(i => rSet_0(i).tag === tag_0
                                                && io.in_0.bp_fire && rSet_0(i).dirty))
    val hits_1 = VecInit((0 until Btb_ways).map(i => rSet_1(i).tag === tag_1 
                                                && io.in_1.bp_fire && rSet_1(i).dirty))

    val hit_0 = hits_0.reduce(_ || _)
    val hit_1 = hits_1.reduce(_ || _)

    //更新LRu计数器
    //相当于被替换的优先级，数字小的先被替换
    when(hit_0) {
        val hit_way0 = PriorityEncoder(hits_0)
        for (i <- 0 until Btb_ways) {
            when(i.U === hit_way0) {
                Btb_bank(bank_idx_0)(i).lru_counter := 3.U  // 最近访问
            }.otherwise {
                when(Btb_bank(bank_idx_0)(i).lru_counter > 0.U) {
                    Btb_bank(bank_idx_0)(i).lru_counter := Btb_bank(bank_idx_0)(i).lru_counter - 1.U
                }
            }
        }
    }
    
    when(hit_1) {
        val hit_way1 = PriorityEncoder(hits_1)
        for (i <- 0 until Btb_ways) {
            when(i.U === hit_way1) {
                Btb_bank(bank_idx_1)(i).lru_counter := 3.U  // 最近访问
            }.otherwise {
                when(Btb_bank(bank_idx_1)(i).lru_counter > 0.U) {
                    Btb_bank(bank_idx_1)(i).lru_counter := Btb_bank(bank_idx_1)(i).lru_counter - 1.U
                }
            }
        }
    }

    //------------------针对不同类型的跳转指令选用BTB或者RSB----------------//
    //返回第一个true的值对应的索引
    val hit_way0 = PriorityEncoder(hits_0)
    val hit_way1 = PriorityEncoder(hits_1)

    val rTargets_0 = VecInit((0 until Btb_ways).map(i => rSet_0(i).Target))
    val rTargets_1 = VecInit((0 until Btb_ways).map(i => rSet_1(i).Target))
    val rTypes_0 = VecInit((0 until Btb_ways).map(i => rSet_0(i).Type))
    val rTypes_1 = VecInit((0 until Btb_ways).map(i => rSet_1(i).Type))

    val target_0 = Mux1H(hits_0, rTargets_0)
    val target_1 = Mux1H(hits_1, rTargets_1)
    val brType_0 = Mux1H(hits_0, rTypes_0)
    val brType_1 = Mux1H(hits_1, rTypes_1)
    //-----------------------接入RSB--------------------------//
    val Rsb = Module(new Stack())
    
    val is_B_0 = hit_0 && (brType_0 === 4.U)
    val is_B_1 = hit_1 && (brType_1 === 4.U)
    val is_B = is_B_0 || is_B_1 
    
    Rsb.io.pop_en_0 := is_B_0
    Rsb.io.pop_en_1 := is_B_1
    Rsb.io.push_en := false.B    // 先初始化为false
    Rsb.io.in_data := 0.U        // 先初始化为0

    val B_en_0 = Rsb.io.out_en_0
    val B_en_1 = Rsb.io.out_en_1
    val B_Target_0 = Rsb.io.out_data_0
    val B_Target_1 = Rsb.io.out_data_1

    io.out_0.brTarget := Mux(is_B_0 && B_en_0, B_Target_0, target_0)
    io.out_1.brTarget := Mux(is_B_1 && B_en_1, B_Target_1, target_1)

    io.out_0.brType := brType_0
    io.out_1.brType := brType_1

    //数据有效取决于是否命中
    //todo 其实不需要valid位(？或者说是需要给后面阶段对比)，pf阶段如果brTaken == 0,默认+4或者+8
    io.out_0.valid := hit_0 || B_en_0
    io.out_1.valid := hit_1 || B_en_1

        //-----------------------------更新策略-----------------------------//
    // 第一级：计算更新信息
    val wSet_u = Wire(Vec(Btb_ways, new Btb_entry))     
    val bank_idx_u = Wire(UInt(log2Ceil(Btb_sets).W))
    val tag_u = Wire(UInt(tagsize.W))
    val hits_u = VecInit((0 until Btb_ways).map(i => (wSet_u(i).tag === tag_u) && wSet_u(i).dirty))

    bank_idx_u := get_idx(io.update.update_pc, 32, idx_len) & (Btb_sets - 1).U
    wSet_u := Btb_bank(bank_idx_u) 
    tag_u := io.update.update_pc(31, 32-tagsize)

    // 计算要写入的way（组合逻辑）
    val w_way = Wire(UInt(1.W))
    when(hits_u.reduce(_ || _)) {
        w_way := Mux(hits_u(0), 0.U, 1.U)
    }.elsewhen(!wSet_u(0).dirty) {
        w_way := 0.U
    }.elsewhen(!wSet_u(1).dirty) {
        w_way := 1.U
    }.otherwise {
        val lru_way = Mux(wSet_u(0).lru_counter < wSet_u(1).lru_counter, 0.U, 1.U)
        w_way := lru_way
    }

    // 第二级：Pipeline寄存器，延迟一拍
    val update_valid_r = RegNext(io.update.require, false.B)
    val bank_idx_u_r = RegNext(bank_idx_u, 0.U)
    val tag_u_r = RegNext(tag_u, 0.U)
    val w_way_r = RegNext(w_way, 0.U)
    val br_type_r = RegNext(io.update.br_type, 0.U)
    val brTarget_r = RegNext(io.update.brTarget, 0.U)
    val update_pc_r = RegNext(io.update.update_pc, 0.U)

    // RSB控制（当前周期就要控制，不能延迟）
    when(io.update.require) {
        when(io.update.br_type === 0.U) {
            Rsb.io.push_en := false.B
        }.elsewhen(io.update.br_type === 2.U) {
            Rsb.io.push_en := true.B
        }.otherwise {
            Rsb.io.push_en := false.B
        }
        Rsb.io.in_data := io.update.update_pc + 4.U
    }.otherwise {
        Rsb.io.push_en := false.B
        Rsb.io.in_data := 0.U
    }

    // 第三级：实际写入BTB（下一周期）
    when(update_valid_r) {
        when(br_type_r =/= 0.U) {  // 只有分支指令才写入BTB
            Btb_bank(bank_idx_u_r)(w_way_r).Target := brTarget_r
            Btb_bank(bank_idx_u_r)(w_way_r).tag := tag_u_r
            Btb_bank(bank_idx_u_r)(w_way_r).Type := br_type_r
            Btb_bank(bank_idx_u_r)(w_way_r).dirty := true.B
        }
    }
    // ================= 计数器优化实现 ================= //
    // Stage1寄存输入信号，消除毛刺，按脉冲只记一次
    //todo 计数分支的信号错误，应当根据Pc类型进行计数
    val fire0_r = RegNext(brType_0 =/= 0.U, 0.B)
    val fire1_r = RegNext(brType_1 =/= 0.U, 0.B)
    val hit0_r  = RegNext(hit_0 && brType_0 =/= 0.U, false.B)
    val hit1_r  = RegNext(hit_1 && brType_0 =/= 0.U, false.B)

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
    // when(io.in_0.bp_fire || io.in_1.bp_fire) {
    //     printf(p"tag_0 = ${tag_0}, io.in_0.bp_fire = ${io.in_0.bp_fire},io.in_1.bp_fire = ${io.in_1.bp_fire}\n")
    //     printf(p"bank_idx_0 = ${bank_idx_0}\n")
    //     printf(p"hit_0 = ${hit_0}, hit_1 = ${hit_1}\n")
    //     printf(p"counter_all = ${counter_all}, counter_hit = ${counter_hit}\n")
    //     for (i <- 0 until Btb_ways) {
    //         printf(p"Btb_bank($bank_idx_0)($i).tag = ${Btb_bank(bank_idx_0)(i).tag}, dirty = ${Btb_bank(bank_idx_0)(i).dirty}\n")
    //     }
    // }

}

    trait  Predictor_Queue {
        val nPHT = 512  // 增加PHT大小
        val PHTLEN = log2Ceil(nPHT)
        val nBHT = 512  // 增加BHT大小
        val BHTLEN = log2Ceil(nBHT)
        val BHRLEN = 8 
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
        })

        val bhtBank = RegInit(VecInit(Seq.fill(nBHT)
                                            (0.U.asTypeOf(new Bht_Entry))))
        val phtBank = RegInit(VecInit(Seq.fill(nPHT)
                                            (0.U.asTypeOf(new Pht_Entry))))
        dontTouch(bhtBank)
        dontTouch(phtBank)
        val bhtBank_idx_0 = Wire(UInt(BHTLEN.W))
        val bhtBank_idx_1 = Wire(UInt(BHTLEN.W))
        val phtBank_idx_0 = Wire(UInt(PHTLEN.W))
        val phtBank_idx_1 = Wire(UInt(PHTLEN.W))

        bhtBank_idx_0 := get_idx(io.pc_in0,32,BHTLEN) & (nBHT-1).U
        bhtBank_idx_1 := get_idx(io.pc_in1,32,BHTLEN) & (nBHT-1).U

        val bhr_0 = bhtBank(bhtBank_idx_0).bhr
        val bhr_1 = bhtBank(bhtBank_idx_1).bhr

        val pc_hash_0 = get_idx(io.pc_in0, 32, PHTLEN) & (nPHT-1).U
        val pc_hash_1 = get_idx(io.pc_in1, 32, PHTLEN) & (nPHT-1).U
        
        phtBank_idx_0 := pc_hash_0 ^ bhr_0(PHTLEN-2, 0)
        phtBank_idx_1 := pc_hash_1 ^ bhr_1(PHTLEN-2, 0)

        io.Taken_0 := phtBank(phtBank_idx_0).ctr(1) && phtBank(phtBank_idx_0).valid
        io.Taken_1 := phtBank(phtBank_idx_1).ctr(1) && phtBank(phtBank_idx_1).valid

        //---------------------------- 更新策略 --------------------------//
        // 第一级：计算更新信息（当前周期）
        val bhtBank_idx_u = Wire(UInt(BHTLEN.W))
        val phtBank_idx_u = Wire(UInt(PHTLEN.W))
        val bhr_u = Wire(UInt(BHRLEN.W))
        val ctr_u = Wire(UInt(2.W))

        bhtBank_idx_u := get_idx(io.update.Pre_pc, 32, BHTLEN) & (nBHT-1).U
        bhr_u := bhtBank(bhtBank_idx_u).bhr
        val pc_hash_u = get_idx(io.update.Pre_pc, 32, PHTLEN) & (nPHT-1).U
        phtBank_idx_u := pc_hash_u ^ bhr_u(PHTLEN-2, 0)
        ctr_u := phtBank(phtBank_idx_u).ctr

        // 第二级：Pipeline寄存器，延迟一拍
        val update_valid_r = RegNext(io.update.valid, false.B)
        val bhtBank_idx_u_r = RegNext(bhtBank_idx_u, 0.U)
        val phtBank_idx_u_r = RegNext(phtBank_idx_u, 0.U)
        val bhr_u_r = RegNext(bhr_u, 0.U)
        val ctr_u_r = RegNext(ctr_u, 0.U)
        val brTaken_r = RegNext(io.update.brTaken, false.B)
        val bht_valid_r = RegNext(bhtBank(bhtBank_idx_u).valid, false.B)
        val pht_valid_r = RegNext(phtBank(phtBank_idx_u).valid, false.B)

        // 第三级：实际更新（下一周期）
        when(update_valid_r) {
            // 更新BHT
            when(bht_valid_r) {
                bhtBank(bhtBank_idx_u_r).bhr := Cat(bhr_u_r(BHRLEN - 2, 0), brTaken_r)
            }.otherwise {
                bhtBank(bhtBank_idx_u_r).bhr := Cat(0.U((BHRLEN-1).W), brTaken_r) 
                bhtBank(bhtBank_idx_u_r).valid := true.B
            }

            // 更新PHT
            when(pht_valid_r) {
                phtBank(phtBank_idx_u_r).ctr := BP_update(ctr_u_r, 2, brTaken_r)
            }.otherwise {
                phtBank(phtBank_idx_u_r).ctr := BP_update(0.U, 2, brTaken_r)
                phtBank(phtBank_idx_u_r).valid := true.B
            }
        }
    }
    // 栈定义
    class Stack extends Module{
        val STACK_SIZE = 16     // 增加栈大小
        val ADDR_WIDTH = log2Ceil(STACK_SIZE)
        
        val io = IO(new Bundle{
            val push_en = Input(Bool())
            val pop_en_0 = Input(Bool())
            val pop_en_1 = Input(Bool())
            val in_data = Input(UInt(32.W))
            val out_en_0 = Output(Bool())
            val out_en_1 = Output(Bool())
            val out_data_0 = Output(UInt(32.W))
            val out_data_1 = Output(UInt(32.W))
        })
        //输入:压栈信号及其数据；出栈信号
        //输出:出栈数据及其是否有效  //*有效位是为了在取不出来的情况下给出信号
        
        val ptr = RegInit(0.U(ADDR_WIDTH.W))
        val stack_bank = RegInit(VecInit(Seq.fill(STACK_SIZE)(0.U(32.W))))
        
        // 改进的栈状态检测
        val empty = ptr === 0.U
        val full = ptr === (STACK_SIZE-1).U
        val has_one = ptr === 1.U
        val has_two_or_more = ptr >= 2.U
        
        // 操作逻辑
        val pop_count = Wire(UInt(2.W))
        val push_count = Wire(UInt(1.W))
        
        pop_count := io.pop_en_0.asUInt + io.pop_en_1.asUInt
        push_count := io.push_en.asUInt
        
        // 计算新的指针位置
        val new_ptr = Wire(UInt(ADDR_WIDTH.W))
        val net_change = push_count.asSInt - pop_count.asSInt
        
        when(net_change > 0.S && !full) {
            new_ptr := ptr + net_change.asUInt
        }.elsewhen(net_change < 0.S && ptr >= (-net_change).asUInt) {
            new_ptr := ptr - (-net_change).asUInt
        }.otherwise {
            new_ptr := ptr
        }
        
        // 更新指针和栈内容
        when(io.push_en && !full) {
            stack_bank(ptr) := io.in_data
        }
        
        ptr := new_ptr
        
        // 改进的输出逻辑
        val can_pop_0 = !empty && io.pop_en_0
        val can_pop_1 = io.pop_en_1 && Mux(io.pop_en_0, has_two_or_more, !empty)
        
        // 输出数据
        when(can_pop_0 && can_pop_1) {
            // 同时弹出两个
            io.out_data_0 := stack_bank(ptr - 1.U)
            io.out_data_1 := stack_bank(ptr - 2.U)
        }.elsewhen(can_pop_0) {
            // 只弹出第一个
            io.out_data_0 := stack_bank(ptr - 1.U)
            io.out_data_1 := 0.U
        }.elsewhen(can_pop_1) {
            // 只弹出第二个
            io.out_data_0 := 0.U
            io.out_data_1 := stack_bank(ptr - 1.U)
        }.otherwise {
            io.out_data_0 := 0.U
            io.out_data_1 := 0.U
        }
        
        io.out_en_0 := can_pop_0
        io.out_en_1 := can_pop_1
        
        // 调试信息
        // when(io.push_en || io.pop_en_0 || io.pop_en_1) {
        //     printf(p"Stack: ptr=$ptr, push=${io.push_en}, pop0=${io.pop_en_0}, pop1=${io.pop_en_1}\n")
        //     printf(p"Stack: out_en_0=${io.out_en_0}, out_en_1=${io.out_en_1}\n")
        //     printf(p"Stack: out_data_0=${Hexadecimal(io.out_data_0)}, out_data_1=${Hexadecimal(io.out_data_1)}\n")
        // }
    }
