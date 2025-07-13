# ToDo

## wzx

- [X] 将tlb和mmu解耦，支持生成带映射翻译模式的电路
- [X] 对icache使用预测cache->pc+8遇到uncache重取指策略
- [ ] bru后边可以跟个lsu发射，只不过在ls级要对这些东西取消，（不在ex取消因为会产生关键路径）
- [X] 双发率32%->36%->30%
- [X] 减少阻塞发生的可能
- [X] 前递目标寄存器为0时不阻塞,load指令要判断目标地址是否相同再阻塞
- [X] 添加性能计数器api
- [X] 流水线增加性能计数器
- [X] 跑通coremark测试ipc能提多少 ipc=0.713
- [X] 增加tlb接口
- [ ] 写一篇提序笔记

## zyf

- [X] 添加uncache
- [X] 添加cacop
- [ ] 优化cache

## ydl

- [X] 合并分支预测ubtb到流水线中
- [ ] 分析ubtb预测结果
- [ ] 添加tage



- [ ] 添加BTB
