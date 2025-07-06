# 移植vivado的tips

## 1.检查makefile脚本

项目主要基于wsl2，基于wsl2的跨文件特性，我们可以轻松的将工程文件复制到windows目录下，因而我们可以用脚本把Verilog文件快速迁移至vivado工程verilog文件夹下

```makefile
vivado:
	rm -rf ./vsrc/*
	mill -i __.test.runMain TopMain --target-dir ./vsrc
	cp ./playground/src/CopyVerilog/* ./vsrc
	mv ./vsrc/*  /mnt/e/la-cputest/cdp_ede_local/mycpu_env/myCPU

#将"/mnt/e/la-cputest/cdp_ede_local/mycpu_env/myCPU"修改为你windows自己的vivado的verilog工程目录
```

## 2.替换vivado仿真顶层

用"liangyi/"目录下的"vivadoDualTop/"替换vivado里相应的文件

```
mycpu_tb.v
soc_lite_top.v
```

## 3.修改Config

修改liangyi/playground/src/Config.scala的部分宏如下

```scala
object GenCtrl { 
  def FAST_SIM=false
  //NOTE:乘除法器使用“*” “/”加速仿真速度
  def USE_DIFF=false
  //NOTE:控制是否生成DIFFTEST电路
}
```

## 4.生成对应电路

在liangyi目录下输入make vivado开启您的vivado之旅

tips:目前在soc_axi的exp11上测试通过