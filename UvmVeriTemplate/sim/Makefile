 #---------------------------------------------------------------------------------
 #                Copyright (c) 2022 
 #                ALL RIGHTS RESERVED
 #---------------------------------------------------------------------------------
 #Filename      : Makefile
 #Author        : AiF
 #Created On    : 2022-05-14 16:11
 #Last Modified : 
 #---------------------------------------------------------------------------------
 #Description   : 


 #---------------------------------------------------------------------------------

UVM_HOME	= /home/yangli/uvm/uvm

include /home/yangli/uvm/uvm/examples/Makefile.vcs
#TESTNAME = wr_with_reset_test
#TESTNAME = base_test    allport_test
TESTNAME = allport_test
TIME = `date +%y%m%d%H`
SEED = $(TIME)
# -l ./top_lib/$(TESTNAME)_simv/vcs_compile_$(TESTNAME).log
run : comp sim


MYVCS = vcs -R \
	-full64 -cpp g++-4.8 -cc gcc-4.8 -LDFLAGS -Wl,--no-as-needed \
	+v2k -sverilog -timescale=1ns/1ns \
	+vpi 
#	+UVM_OBJECTION_TRACE \
#+incdir+$(UVM_HOME)/src $(UVM_HOME)/src/uvm.sv \
#$(UVM_HOME)/src/dpi/uvm_dpi.cc -CFLAGS -DVCS
#uvm_incdir =+incdir+${UVM_HOME}/src+${UVM_HOME}/src/vcs
#uvm_file = ${UVM_HOME}/src/uvm_pkg.sv
#UVM_COMPILE = ${UVM_HOME}/src/dpi/uvm_dpi.cc \
#    ${uvm_file}\
#    -CFLAGS \
#    -DVCS \
#    $(uvm_incdir) \
#    $(uvm_comp_option)

comp :
	@echo "Create the directory for the testbench"
	mkdir -p top_lib/$(TESTNAME)
	@echo "Compile the testbench"
	$(MYVCS) -ntb_opts uvm \
	+nospecify +notimingcheck -debug_access+all \
	+vcs+lic+wait \
	+vcsd +memcbk -kdb -lca \
	-o top_lib/$(TESTNAME)_simv \
	+UVM_TESTNAME=$(TESTNAME) +fsdb+region \
	-l ./top_lib/$(TESTNAME)/vcs_compile_$(TESTNAME).log \
	-f ./filelist/rtl.f \
	-f ./filelist/uvm_tb.f \
	-f ./filelist/agents.f \
	-f ./filelist/env.f \
	-f ./filelist/test.f \
	-f ./filelist/tb.f 

sim:
	./top_lib/$(TESTNAME)_simv +fsdb+delta +UVM_TESTNAME=$(TESTNAME) +fsdb+sva_success -l ./$(TESTNAME)/$(TESTNAME)_sim.log 

nWave:
	nWave -ssf $(TESTNAME).fsdb -sswr fifo_signal.rc &
cleanall:
		rm -rf csrc *compile* *.h \
		nWaveLog \
		verdiLog ucli.* DVEfile* \
		*.fsdb \
		cover \
		top_lib

#sudo rm	-rf top_lib

dve:
	dve -full64 &

cov:
	urg -dir ./top_lib/*.vdb -dbname cover/merge.vdb -full64 -report cover

verdi:
	verdi -full64 -nologo \
	-ssf $(TESTNAME).fsdb \
	-sswr signal.rc \
	-rcFile novas.rc 
view:
	cd $(TESTNAME) \ ls *.log

GIT=auto_commit

git:
	git add ./filelist/*.f 
	git add Makefile
#git add ../rtl/*.v
#git add ../agents/**/*.sv
#包含uvm_tb下所有的自文件夹内的文件
#git add ../uvm_tb/**/*.sv
	git commit -m "$(GIT)"
	git push 