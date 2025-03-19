// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vtop.h for the primary calling header

#include "Vtop___024root.h"
#include "Vtop__Syms.h"

//==========

VL_INLINE_OPT void Vtop___024root___combo__TOP__1(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___combo__TOP__1\n"); );
    // Body
    vlSelf->result = 0U;
    vlSelf->c = 0U;
    vlSelf->over = 0U;
    if ((0U == (IData)(vlSelf->func))) {
        if ((8U & ((IData)(vlSelf->a) & (IData)(vlSelf->b)))) {
            vlSelf->top__DOT__a1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->a))));
            vlSelf->top__DOT__b1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->b))));
        } else if ((8U & (IData)(vlSelf->a))) {
            vlSelf->top__DOT__a1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->a))));
            vlSelf->top__DOT__b1 = vlSelf->b;
            vlSelf->top__DOT__a1 = (8U | (IData)(vlSelf->top__DOT__a1));
        } else if ((8U & ((IData)(vlSelf->a) & (IData)(vlSelf->b)))) {
            vlSelf->top__DOT__a1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->a))));
            vlSelf->top__DOT__b1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->b))));
        } else {
            vlSelf->top__DOT__a1 = vlSelf->a;
            vlSelf->top__DOT__b1 = vlSelf->b;
        }
        vlSelf->c = (1U & (((IData)(vlSelf->top__DOT__a1) 
                            + (IData)(vlSelf->top__DOT__b1)) 
                           >> 4U));
        vlSelf->result = (0xfU & ((IData)(vlSelf->top__DOT__a1) 
                                  + (IData)(vlSelf->top__DOT__b1)));
        if ((8U & (IData)(vlSelf->result))) {
            vlSelf->result = (0xfU & ((IData)(vlSelf->result) 
                                      - (IData)(1U)));
            vlSelf->result = (0xfU & (~ (IData)(vlSelf->result)));
            vlSelf->result = (8U | (IData)(vlSelf->result));
        }
        vlSelf->over = (((1U & ((IData)(vlSelf->a) 
                                >> 3U)) == (1U & ((IData)(vlSelf->b) 
                                                  >> 3U))) 
                        & ((1U & ((IData)(vlSelf->result) 
                                  >> 3U)) != (1U & 
                                              ((IData)(vlSelf->a) 
                                               >> 3U))));
    } else if ((1U == (IData)(vlSelf->func))) {
        if ((8U & ((IData)(vlSelf->a) & (IData)(vlSelf->b)))) {
            vlSelf->top__DOT__a1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->a))));
            vlSelf->top__DOT__b1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->b))));
        } else if ((8U & (IData)(vlSelf->a))) {
            vlSelf->top__DOT__a1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->a))));
            vlSelf->top__DOT__b1 = vlSelf->b;
            vlSelf->top__DOT__a1 = (8U | (IData)(vlSelf->top__DOT__a1));
        } else if ((8U & ((IData)(vlSelf->a) & (IData)(vlSelf->b)))) {
            vlSelf->top__DOT__a1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->a))));
            vlSelf->top__DOT__b1 = (0xfU & ((IData)(1U) 
                                            + (~ (IData)(vlSelf->b))));
        } else {
            vlSelf->top__DOT__a1 = vlSelf->a;
            vlSelf->top__DOT__b1 = vlSelf->b;
        }
        vlSelf->c = (1U & (((IData)(1U) + ((IData)(vlSelf->top__DOT__a1) 
                                           + (~ (IData)(vlSelf->top__DOT__b1)))) 
                           >> 4U));
        vlSelf->result = (0xfU & ((IData)(1U) + ((IData)(vlSelf->top__DOT__a1) 
                                                 + 
                                                 (~ (IData)(vlSelf->top__DOT__b1)))));
        if ((8U & (IData)(vlSelf->result))) {
            vlSelf->result = (0xfU & ((IData)(vlSelf->result) 
                                      - (IData)(1U)));
            vlSelf->result = (0xfU & (~ (IData)(vlSelf->result)));
            vlSelf->result = (8U | (IData)(vlSelf->result));
        }
        vlSelf->over = (((1U & ((IData)(vlSelf->a) 
                                >> 3U)) == (1U & ((IData)(vlSelf->b) 
                                                  >> 3U))) 
                        & ((1U & ((IData)(vlSelf->result) 
                                  >> 3U)) != (1U & 
                                              ((IData)(vlSelf->a) 
                                               >> 3U))));
    } else if ((2U == (IData)(vlSelf->func))) {
        vlSelf->result = (0xfU & (~ (IData)(vlSelf->a)));
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = vlSelf->a;
        vlSelf->top__DOT__b1 = vlSelf->b;
    } else if ((3U == (IData)(vlSelf->func))) {
        vlSelf->result = ((IData)(vlSelf->a) & (IData)(vlSelf->b));
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = vlSelf->a;
        vlSelf->top__DOT__b1 = vlSelf->b;
    } else if ((4U == (IData)(vlSelf->func))) {
        vlSelf->result = ((IData)(vlSelf->a) | (IData)(vlSelf->b));
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = vlSelf->a;
        vlSelf->top__DOT__b1 = vlSelf->b;
    } else if ((5U == (IData)(vlSelf->func))) {
        vlSelf->result = ((IData)(vlSelf->a) ^ (IData)(vlSelf->b));
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = vlSelf->a;
        vlSelf->top__DOT__b1 = vlSelf->b;
    } else if ((6U == (IData)(vlSelf->func))) {
        vlSelf->result = (((IData)(vlSelf->a) < (IData)(vlSelf->b))
                           ? 1U : 0U);
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = vlSelf->a;
        vlSelf->top__DOT__b1 = vlSelf->b;
    } else if ((7U == (IData)(vlSelf->func))) {
        vlSelf->result = (((IData)(vlSelf->a) == (IData)(vlSelf->b))
                           ? 1U : 0U);
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = vlSelf->a;
        vlSelf->top__DOT__b1 = vlSelf->b;
    } else {
        vlSelf->result = 0U;
        vlSelf->over = 0U;
        vlSelf->c = 0U;
        vlSelf->top__DOT__a1 = 0U;
        vlSelf->top__DOT__b1 = 0U;
    }
}

void Vtop___024root___eval(Vtop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vtop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vtop___024root___eval\n"); );
    // Body
    Vtop___024root___combo__TOP__1(vlSelf);
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
    if (VL_UNLIKELY((vlSelf->rst & 0xfeU))) {
        Verilated::overWidthError("rst");}
    if (VL_UNLIKELY((vlSelf->func & 0xf8U))) {
        Verilated::overWidthError("func");}
    if (VL_UNLIKELY((vlSelf->a & 0xf0U))) {
        Verilated::overWidthError("a");}
    if (VL_UNLIKELY((vlSelf->b & 0xf0U))) {
        Verilated::overWidthError("b");}
}
#endif  // VL_DEBUG
