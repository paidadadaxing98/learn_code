#built a dir for new project

DIR_NAME = keyboard
#chage name to new_project_name
PATH_DIR = ~/ysyx-workbench/nvboard/project/$(DIR_NAME)


build:
	mkdir $(PATH_DIR)
	mkdir $(PATH_DIR)/vsrc    $(PATH_DIR)/csrc  
	mkdir $(PATH_DIR)/constr
	cp /home/zy/ysyx-workbench/nvboard/example/Makefile   $(PATH_DIR)/
	touch $(PATH_DIR)/constr/top.nxdc
	touch $(PATH_DIR)/vsrc/top.v
clean:
	rm  -rf $(PATH_DIR)/vsrc   $(PATH_DIR)/csrc   
	rm 	-rf $(PATH_DIR)/constr
	rm  $(PATH_DIR)/Makefile  $(PATH_DIR)/constr/top.nxdc

.PHONY:build clean