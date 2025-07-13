
chiplab:
	rm -r $(CHIPLAB_HOME)/IP/myCPU
	rm -rf ./vsrc/*
	mkdir -p $(CHIPLAB_HOME)/IP/myCPU
	mill -i __.test.runMain TopMain --target-dir ./vsrc
	cp ./vsrc/*  $(CHIPLAB_HOME)/IP/myCPU/
	cp ./playground/src/CopyVerilog/* $(CHIPLAB_HOME)/IP/myCPU/


verilog:
	rm -rf ./vsrc/*
	mkdir -p ./vsrc
	mill -i __.test.runMain TopMain --target-dir ./vsrc


#在~/.bashrc中添加环境变量 FUNC_TEST_PATH 和 PERF_TEST_PATH
# export FUNC_TEST_PATH="/your/func_test/path"
# export PERF_TEST_PATH="/your/perf_test/path"
VIVADO_POJECT_IP_PATH?=null #TODO
FUNC_TEST_PATH?=null
PERF_TEST_PATH?=null
SOC_PATH?=../IP/myCPU
func:
	rm -rf ./vsrc/*
	mill -i __.test.runMain TopMain --target-dir ./vsrc
	cp ./playground/src/CopyVerilog/* ./vsrc
	rm ./vsrc/TagvRAM.sv ./vsrc/DataRAM.sv
	mkdir -p ${FUNC_TEST_PATH}
	rm -rf ${FUNC_TEST_PATH}/*
	cp ./vsrc/*  ${FUNC_TEST_PATH}


perf:
	rm -rf ./vsrc/*
	mill -i __.test.runMain TopMain --target-dir ./vsrc
	cp ./playground/src/CopyVerilog/* ./vsrc
	rm ./vsrc/TagvRAM.sv ./vsrc/DataRAM.sv
	mkdir -p ${PERF_TEST_PATH}
	rm -rf ${PERF_TEST_PATH}/*
	cp ./vsrc/*  ${PERF_TEST_PATH}

soc:
	rm -rf ./vsrc/*
	mill -i __.test.runMain TopMain --target-dir ./vsrc
	cp ./playground/src/CopyVerilog/* ./vsrc
	rm ./vsrc/TagvRAM.sv ./vsrc/DataRAM.sv
	mkdir -p ${SOC_PATH}
	rm -rf ${SOC_PATH}/*
	cp ./vsrc/*  ${SOC_PATH}
	cp -r ./xilinx_ip ${SOC_PATH}/xilinx_ip


ref:
	rm -r $(CHIPLAB_HOME)/IP/myCPU
	cp -r $(CHIPLAB_HOME)/TwoCPU-ref $(CHIPLAB_HOME)/IP/myCPU
	cp  $(CHIPLAB_HOME)/TwoCPU-ref/mmu/*.sv $(CHIPLAB_HOME)/IP/myCPU
	cp  $(CHIPLAB_HOME)/TwoCPU-ref/sim/*.sv $(CHIPLAB_HOME)/IP/myCPU
	cp  $(CHIPLAB_HOME)/TwoCPU-ref/pred/*.sv $(CHIPLAB_HOME)/IP/myCPU

cnt_v := $(shell find vsrc/ -name "*.sv" -or -name "*.scala" -or -name "*.v" | xargs grep -ve "^$$" | wc -l)
cnt_s := $(shell find playground/ -name "*.sv" -or -name "*.scala" -or -name "*.v"| xargs grep -ve "^$$" | wc -l)
count:
	@echo $(cnt_v)-sverilog-lines in /vsrc	  
	@echo $(cnt_s)--chisel--lines in /playground
