// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "Vtop___024root.h"
#include "Vtop__Syms.h"

//==========


void Vtop___024root___ctor_var_reset(Vtop___024root* vlSelf);

Vtop___024root::Vtop___024root(const char* _vcname__)
    : VerilatedModule(_vcname__)
 {
    // Reset structure values
    Vtop___024root___ctor_var_reset(this);
}

void Vtop___024root::__Vconfigure(Vtop__Syms* _vlSymsp, bool first) {
    if (false && first) {}  // Prevent unused
    this->vlSymsp = _vlSymsp;
}

Vtop___024root::~Vtop___024root() {
}

extern const VlUnpacked<SData/*15:0*/, 512> Vtop__ConstPool__TABLE_cbee3d89_0;
extern const VlUnpacked<CData/*7:0*/, 256> Vtop__ConstPool__TABLE_20d33d4b_0;

void Vtop___024root___settle__TOP__3(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___settle__TOP__3\n"); );
    // Variables
    CData/*7:0*/ __Vtableidx2;
    CData/*7:0*/ __Vtableidx3;
    CData/*7:0*/ __Vtableidx6;
    CData/*7:0*/ __Vtableidx7;
    CData/*7:0*/ __Vtableidx8;
    CData/*7:0*/ __Vtableidx9;
    SData/*15:0*/ top__DOT__keycode1__DOT__seg_inst__DOT__in_r;
    SData/*15:0*/ top__DOT__asciicode1__DOT__seg_inst__DOT__in_r;
    SData/*15:0*/ top__DOT__key_count1__DOT__seg_inst__DOT__in_r;
    SData/*8:0*/ __Vtableidx1;
    SData/*8:0*/ __Vtableidx5;
    // Body
    vlSelf->top__DOT____Vcellinp__asciicode1__resetn 
        = (1U & (~ (IData)(vlSelf->resetn)));
    top__DOT__key_count1__DOT__seg_inst__DOT__in_r 
        = ((0xf00U & ((IData)(vlSelf->top__DOT__num) 
                      << 4U)) | (0xfU & (IData)(vlSelf->top__DOT__num)));
    __Vtableidx1 = (((IData)(vlSelf->top__DOT__scancode) 
                     << 1U) | (IData)(vlSelf->top__DOT__light));
    top__DOT__keycode1__DOT__seg_inst__DOT__in_r = 
        Vtop__ConstPool__TABLE_cbee3d89_0[__Vtableidx1];
    __Vtableidx5 = (((IData)(vlSelf->top__DOT__ascii) 
                     << 1U) | (IData)(vlSelf->top__DOT__light));
    top__DOT__asciicode1__DOT__seg_inst__DOT__in_r 
        = Vtop__ConstPool__TABLE_cbee3d89_0[__Vtableidx5];
    __Vtableidx8 = (0xffU & (IData)(top__DOT__key_count1__DOT__seg_inst__DOT__in_r));
    vlSelf->seg4 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx8];
    __Vtableidx9 = (0xffU & ((IData)(top__DOT__key_count1__DOT__seg_inst__DOT__in_r) 
                             >> 8U));
    vlSelf->seg5 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx9];
    __Vtableidx2 = (0xffU & (IData)(top__DOT__keycode1__DOT__seg_inst__DOT__in_r));
    vlSelf->seg0 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx2];
    __Vtableidx3 = (0xffU & ((IData)(top__DOT__keycode1__DOT__seg_inst__DOT__in_r) 
                             >> 8U));
    vlSelf->seg1 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx3];
    __Vtableidx6 = (0xffU & (IData)(top__DOT__asciicode1__DOT__seg_inst__DOT__in_r));
    vlSelf->seg2 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx6];
    __Vtableidx7 = (0xffU & ((IData)(top__DOT__asciicode1__DOT__seg_inst__DOT__in_r) 
                             >> 8U));
    vlSelf->seg3 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx7];
}

void Vtop___024root___eval_initial(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_initial\n"); );
    // Body
    vlSelf->__Vclklast__TOP__clk = vlSelf->clk;
    vlSelf->__Vclklast__TOP____VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn 
        = vlSelf->__VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn;
}

void Vtop___024root___eval_settle(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_settle\n"); );
    // Body
    Vtop___024root___settle__TOP__3(vlSelf);
}

void Vtop___024root___final(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___final\n"); );
}

void Vtop___024root___ctor_var_reset(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___ctor_var_reset\n"); );
    // Body
    vlSelf->clk = 0;
    vlSelf->resetn = 0;
    vlSelf->ps2_clk = 0;
    vlSelf->ps2_data = 0;
    vlSelf->seg0 = 0;
    vlSelf->seg1 = 0;
    vlSelf->seg2 = 0;
    vlSelf->seg3 = 0;
    vlSelf->seg4 = 0;
    vlSelf->seg5 = 0;
    vlSelf->top__DOT__ascii = 0;
    vlSelf->top__DOT__scancode = 0;
    vlSelf->top__DOT__num = 0;
    vlSelf->top__DOT__light = 0;
    vlSelf->top__DOT____Vcellinp__asciicode1__resetn = 0;
    vlSelf->top__DOT__keyboard__DOT__buffer = 0;
    vlSelf->top__DOT__keyboard__DOT__count = 0;
    vlSelf->top__DOT__keyboard__DOT__ps2_clk_sync = 0;
    vlSelf->top__DOT__keyboard__DOT__pressed = 0;
    vlSelf->top__DOT__keyboard__DOT____Vlvbound1 = 0;
    vlSelf->__Vdly__top__DOT__scancode = 0;
    vlSelf->__VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn = 0;
    vlSelf->__Vchglast__TOP__top__DOT____Vcellinp__asciicode1__resetn = 0;
}
