// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "Vtop___024root.h"
#include "Vtop__Syms.h"

//==========

extern const VlUnpacked<CData/*7:0*/, 256> Vtop__ConstPool__TABLE_20d33d4b_0;

VL_INLINE_OPT void Vtop___024root___sequent__TOP__1(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___sequent__TOP__1\n"); );
    // Variables
    CData/*7:0*/ __Vtableidx8;
    CData/*7:0*/ __Vtableidx9;
    CData/*2:0*/ __Vdly__top__DOT__keyboard__DOT__ps2_clk_sync;
    CData/*3:0*/ __Vdly__top__DOT__keyboard__DOT__count;
    SData/*15:0*/ top__DOT__key_count1__DOT__seg_inst__DOT__in_r;
    // Body
    __Vdly__top__DOT__keyboard__DOT__ps2_clk_sync = vlSelf->top__DOT__keyboard__DOT__ps2_clk_sync;
    __Vdly__top__DOT__keyboard__DOT__count = vlSelf->top__DOT__keyboard__DOT__count;
    vlSelf->__Vdly__top__DOT__scancode = vlSelf->top__DOT__scancode;
    __Vdly__top__DOT__keyboard__DOT__ps2_clk_sync = 
        ((6U & ((IData)(vlSelf->top__DOT__keyboard__DOT__ps2_clk_sync) 
                << 1U)) | (IData)(vlSelf->ps2_clk));
    if (vlSelf->resetn) {
        vlSelf->top__DOT__num = 0U;
        vlSelf->top__DOT__keyboard__DOT__pressed = 0U;
        vlSelf->top__DOT__light = 0U;
        __Vdly__top__DOT__keyboard__DOT__count = 0U;
        vlSelf->__Vdly__top__DOT__scancode = 0xffU;
    } else if ((IData)((4U == (6U & (IData)(vlSelf->top__DOT__keyboard__DOT__ps2_clk_sync))))) {
        if ((0xaU == (IData)(vlSelf->top__DOT__keyboard__DOT__count))) {
            if (VL_UNLIKELY((((~ (IData)(vlSelf->top__DOT__keyboard__DOT__buffer)) 
                              & (IData)(vlSelf->ps2_data)) 
                             & VL_REDXOR_32((0x1ffU 
                                             & ((IData)(vlSelf->top__DOT__keyboard__DOT__buffer) 
                                                >> 1U)))))) {
                if ((0xf0U != (IData)(vlSelf->top__DOT__scancode))) {
                    vlSelf->top__DOT__keyboard__DOT__pressed = 0U;
                    vlSelf->top__DOT__light = 1U;
                } else {
                    vlSelf->top__DOT__keyboard__DOT__pressed = 1U;
                    vlSelf->top__DOT__light = 0U;
                }
                if (vlSelf->top__DOT__keyboard__DOT__pressed) {
                    vlSelf->top__DOT__num = (0xffU 
                                             & ((IData)(1U) 
                                                + (IData)(vlSelf->top__DOT__num)));
                }
                VL_WRITEF("receive %x\n",8,(0xffU & 
                                            ((IData)(vlSelf->top__DOT__keyboard__DOT__buffer) 
                                             >> 1U)));
                vlSelf->__Vdly__top__DOT__scancode 
                    = (0xffU & ((IData)(vlSelf->top__DOT__keyboard__DOT__buffer) 
                                >> 1U));
                VL_WRITEF(" num is %3#\n",8,vlSelf->top__DOT__num);
            }
            __Vdly__top__DOT__keyboard__DOT__count = 0U;
            vlSelf->top__DOT__keyboard__DOT__pressed = 0U;
        } else {
            vlSelf->top__DOT__keyboard__DOT____Vlvbound1 
                = vlSelf->ps2_data;
            if (VL_LIKELY((9U >= (IData)(vlSelf->top__DOT__keyboard__DOT__count)))) {
                vlSelf->top__DOT__keyboard__DOT__buffer 
                    = (((~ ((IData)(1U) << (IData)(vlSelf->top__DOT__keyboard__DOT__count))) 
                        & (IData)(vlSelf->top__DOT__keyboard__DOT__buffer)) 
                       | (0x3ffU & ((IData)(vlSelf->top__DOT__keyboard__DOT____Vlvbound1) 
                                    << (IData)(vlSelf->top__DOT__keyboard__DOT__count))));
            }
            __Vdly__top__DOT__keyboard__DOT__count 
                = (0xfU & ((IData)(1U) + (IData)(vlSelf->top__DOT__keyboard__DOT__count)));
        }
    }
    vlSelf->top__DOT__keyboard__DOT__count = __Vdly__top__DOT__keyboard__DOT__count;
    vlSelf->top__DOT__keyboard__DOT__ps2_clk_sync = __Vdly__top__DOT__keyboard__DOT__ps2_clk_sync;
    top__DOT__key_count1__DOT__seg_inst__DOT__in_r 
        = ((0xf00U & ((IData)(vlSelf->top__DOT__num) 
                      << 4U)) | (0xfU & (IData)(vlSelf->top__DOT__num)));
    __Vtableidx8 = (0xffU & (IData)(top__DOT__key_count1__DOT__seg_inst__DOT__in_r));
    vlSelf->seg4 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx8];
    __Vtableidx9 = (0xffU & ((IData)(top__DOT__key_count1__DOT__seg_inst__DOT__in_r) 
                             >> 8U));
    vlSelf->seg5 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx9];
}

extern const VlUnpacked<CData/*7:0*/, 512> Vtop__ConstPool__TABLE_7fc48ed5_0;
extern const VlUnpacked<SData/*15:0*/, 512> Vtop__ConstPool__TABLE_cbee3d89_0;

VL_INLINE_OPT void Vtop___024root___sequent__TOP__2(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___sequent__TOP__2\n"); );
    // Variables
    CData/*7:0*/ __Vtableidx6;
    CData/*7:0*/ __Vtableidx7;
    SData/*15:0*/ top__DOT__asciicode1__DOT__seg_inst__DOT__in_r;
    SData/*8:0*/ __Vtableidx4;
    SData/*8:0*/ __Vtableidx5;
    // Body
    __Vtableidx4 = (((IData)(vlSelf->top__DOT__scancode) 
                     << 1U) | (IData)(vlSelf->top__DOT____Vcellinp__asciicode1__resetn));
    vlSelf->top__DOT__ascii = Vtop__ConstPool__TABLE_7fc48ed5_0
        [__Vtableidx4];
    __Vtableidx5 = (((IData)(vlSelf->top__DOT__ascii) 
                     << 1U) | (IData)(vlSelf->top__DOT__light));
    top__DOT__asciicode1__DOT__seg_inst__DOT__in_r 
        = Vtop__ConstPool__TABLE_cbee3d89_0[__Vtableidx5];
    __Vtableidx6 = (0xffU & (IData)(top__DOT__asciicode1__DOT__seg_inst__DOT__in_r));
    vlSelf->seg2 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx6];
    __Vtableidx7 = (0xffU & ((IData)(top__DOT__asciicode1__DOT__seg_inst__DOT__in_r) 
                             >> 8U));
    vlSelf->seg3 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx7];
}

VL_INLINE_OPT void Vtop___024root___combo__TOP__4(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___combo__TOP__4\n"); );
    // Body
    vlSelf->top__DOT____Vcellinp__asciicode1__resetn 
        = (1U & (~ (IData)(vlSelf->resetn)));
}

VL_INLINE_OPT void Vtop___024root___sequent__TOP__5(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___sequent__TOP__5\n"); );
    // Variables
    CData/*7:0*/ __Vtableidx2;
    CData/*7:0*/ __Vtableidx3;
    SData/*15:0*/ top__DOT__keycode1__DOT__seg_inst__DOT__in_r;
    SData/*8:0*/ __Vtableidx1;
    // Body
    vlSelf->top__DOT__scancode = vlSelf->__Vdly__top__DOT__scancode;
    __Vtableidx1 = (((IData)(vlSelf->top__DOT__scancode) 
                     << 1U) | (IData)(vlSelf->top__DOT__light));
    top__DOT__keycode1__DOT__seg_inst__DOT__in_r = 
        Vtop__ConstPool__TABLE_cbee3d89_0[__Vtableidx1];
    __Vtableidx2 = (0xffU & (IData)(top__DOT__keycode1__DOT__seg_inst__DOT__in_r));
    vlSelf->seg0 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx2];
    __Vtableidx3 = (0xffU & ((IData)(top__DOT__keycode1__DOT__seg_inst__DOT__in_r) 
                             >> 8U));
    vlSelf->seg1 = Vtop__ConstPool__TABLE_20d33d4b_0
        [__Vtableidx3];
}

void Vtop___024root___eval(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval\n"); );
    // Body
    if (((IData)(vlSelf->clk) & (~ (IData)(vlSelf->__Vclklast__TOP__clk)))) {
        Vtop___024root___sequent__TOP__1(vlSelf);
    }
    if ((((IData)(vlSelf->clk) & (~ (IData)(vlSelf->__Vclklast__TOP__clk))) 
         | ((~ (IData)(vlSelf->__VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn)) 
            & (IData)(vlSelf->__Vclklast__TOP____VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn)))) {
        Vtop___024root___sequent__TOP__2(vlSelf);
    }
    Vtop___024root___combo__TOP__4(vlSelf);
    if (((IData)(vlSelf->clk) & (~ (IData)(vlSelf->__Vclklast__TOP__clk)))) {
        Vtop___024root___sequent__TOP__5(vlSelf);
    }
    // Final
    vlSelf->__Vclklast__TOP__clk = vlSelf->clk;
    vlSelf->__Vclklast__TOP____VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn 
        = vlSelf->__VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn;
    vlSelf->__VinpClk__TOP__top__DOT____Vcellinp__asciicode1__resetn 
        = vlSelf->top__DOT____Vcellinp__asciicode1__resetn;
}

QData Vtop___024root___change_request_1(Vtop___024root* vlSelf);

VL_INLINE_OPT QData Vtop___024root___change_request(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___change_request\n"); );
    // Body
    return (Vtop___024root___change_request_1(vlSelf));
}

VL_INLINE_OPT QData Vtop___024root___change_request_1(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___change_request_1\n"); );
    // Body
    // Change detection
    QData __req = false;  // Logically a bool
    __req |= ((vlSelf->top__DOT____Vcellinp__asciicode1__resetn ^ vlSelf->__Vchglast__TOP__top__DOT____Vcellinp__asciicode1__resetn));
    VL_DEBUG_IF( if(__req && ((vlSelf->top__DOT____Vcellinp__asciicode1__resetn ^ vlSelf->__Vchglast__TOP__top__DOT____Vcellinp__asciicode1__resetn))) VL_DBG_MSGF("        CHANGE: /home/zy/ysyx-workbench/nvboard/project/keyboard/vsrc/asciicode.v:4: top.__Vcellinp__asciicode1__resetn\n"); );
    // Final
    vlSelf->__Vchglast__TOP__top__DOT____Vcellinp__asciicode1__resetn 
        = vlSelf->top__DOT____Vcellinp__asciicode1__resetn;
    return __req;
}

#ifdef VL_DEBUG
void Vtop___024root___eval_debug_assertions(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval_debug_assertions\n"); );
    // Body
    if (VL_UNLIKELY((vlSelf->clk & 0xfeU))) {
        Verilated::overWidthError("clk");}
    if (VL_UNLIKELY((vlSelf->resetn & 0xfeU))) {
        Verilated::overWidthError("resetn");}
    if (VL_UNLIKELY((vlSelf->ps2_clk & 0xfeU))) {
        Verilated::overWidthError("ps2_clk");}
    if (VL_UNLIKELY((vlSelf->ps2_data & 0xfeU))) {
        Verilated::overWidthError("ps2_data");}
}
#endif  // VL_DEBUG
