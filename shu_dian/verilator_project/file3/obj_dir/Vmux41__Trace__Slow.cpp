// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Tracing implementation internals
#include "verilated_vcd_c.h"
#include "Vmux41__Syms.h"


void Vmux41___024root__traceInitSub0(Vmux41___024root* vlSelf, VerilatedVcd* tracep) VL_ATTR_COLD;

void Vmux41___024root__traceInitTop(Vmux41___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vmux41__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    {
        Vmux41___024root__traceInitSub0(vlSelf, tracep);
    }
}

void Vmux41___024root__traceInitSub0(Vmux41___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vmux41__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    const int c = vlSymsp->__Vm_baseCode;
    if (false && tracep && c) {}  // Prevent unused
    // Body
    {
        tracep->declBus(c+1,"a", false,-1, 3,0);
        tracep->declBus(c+2,"choose", false,-1, 1,0);
        tracep->declBit(c+3,"out", false,-1);
        tracep->declBus(c+1,"mux41 a", false,-1, 3,0);
        tracep->declBus(c+2,"mux41 choose", false,-1, 1,0);
        tracep->declBit(c+3,"mux41 out", false,-1);
    }
}

void Vmux41___024root__traceFullTop0(void* voidSelf, VerilatedVcd* tracep) VL_ATTR_COLD;
void Vmux41___024root__traceChgTop0(void* voidSelf, VerilatedVcd* tracep);
void Vmux41___024root__traceCleanup(void* voidSelf, VerilatedVcd* /*unused*/);

void Vmux41___024root__traceRegister(Vmux41___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vmux41__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    {
        tracep->addFullCb(&Vmux41___024root__traceFullTop0, vlSelf);
        tracep->addChgCb(&Vmux41___024root__traceChgTop0, vlSelf);
        tracep->addCleanupCb(&Vmux41___024root__traceCleanup, vlSelf);
    }
}

void Vmux41___024root__traceFullSub0(Vmux41___024root* vlSelf, VerilatedVcd* tracep) VL_ATTR_COLD;

void Vmux41___024root__traceFullTop0(void* voidSelf, VerilatedVcd* tracep) {
    Vmux41___024root* const __restrict vlSelf = static_cast<Vmux41___024root*>(voidSelf);
    Vmux41__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    // Body
    {
        Vmux41___024root__traceFullSub0((&vlSymsp->TOP), tracep);
    }
}

void Vmux41___024root__traceFullSub0(Vmux41___024root* vlSelf, VerilatedVcd* tracep) {
    if (false && vlSelf) {}  // Prevent unused
    Vmux41__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    vluint32_t* const oldp = tracep->oldp(vlSymsp->__Vm_baseCode);
    if (false && oldp) {}  // Prevent unused
    // Body
    {
        tracep->fullCData(oldp+1,(vlSelf->a),4);
        tracep->fullCData(oldp+2,(vlSelf->choose),2);
        tracep->fullBit(oldp+3,(vlSelf->out));
    }
}
