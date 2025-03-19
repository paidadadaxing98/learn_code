// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vtop.h for the primary calling header

#ifndef VERILATED_VTOP___024ROOT_H_
#define VERILATED_VTOP___024ROOT_H_  // guard

#include "verilated_heavy.h"

//==========

class Vtop__Syms;

//----------

VL_MODULE(Vtop___024root) {
  public:

    // PORTS
    VL_IN8(clk,0,0);
    VL_IN8(resetn,0,0);
    VL_IN8(ps2_clk,0,0);
    VL_IN8(ps2_data,0,0);
    VL_OUT8(seg0,7,0);
    VL_OUT8(seg1,7,0);
    VL_OUT8(seg2,7,0);
    VL_OUT8(seg3,7,0);
    VL_OUT8(seg4,7,0);
    VL_OUT8(seg5,7,0);

    // LOCAL SIGNALS
    CData/*7:0*/ top__DOT__ascii;
    CData/*7:0*/ top__DOT__scancode;
    CData/*7:0*/ top__DOT__num;
    CData/*0:0*/ top__DOT__light;
    CData/*3:0*/ top__DOT__keyboard__DOT__count;
    CData/*2:0*/ top__DOT__keyboard__DOT__ps2_clk_sync;
    CData/*0:0*/ top__DOT__keyboard__DOT__pressed;
    SData/*9:0*/ top__DOT__keyboard__DOT__buffer;

    // LOCAL VARIABLES
    CData/*0:0*/ top__DOT____Vcellinp__asciicode1__resetn;
    CData/*0:0*/ top__DOT__keyboard__DOT____Vlvbound1;
    CData/*7:0*/ __Vdly__top__DOT__scancode;
    CData/*0:0*/ __VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn;
    CData/*0:0*/ __Vclklast__TOP__clk;
    CData/*0:0*/ __Vclklast__TOP____VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn;
    CData/*0:0*/ __Vchglast__TOP__top__DOT____Vcellinp__asciicode1__resetn;

    // INTERNAL VARIABLES
    Vtop__Syms* vlSymsp;  // Symbol table

    // CONSTRUCTORS
  private:
    VL_UNCOPYABLE(Vtop___024root);  ///< Copying not allowed
  public:
    Vtop___024root(const char* name);
    ~Vtop___024root();

    // INTERNAL METHODS
    void __Vconfigure(Vtop__Syms* symsp, bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);

//----------


#endif  // guard
