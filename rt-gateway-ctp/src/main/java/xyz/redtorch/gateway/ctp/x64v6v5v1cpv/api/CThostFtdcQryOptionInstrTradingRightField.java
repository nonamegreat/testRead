/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v5v1cpv.api;

public class CThostFtdcQryOptionInstrTradingRightField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcQryOptionInstrTradingRightField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcQryOptionInstrTradingRightField obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        jctpv6v5v1cpx64apiJNI.delete_CThostFtdcQryOptionInstrTradingRightField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_InvestorID_get(swigCPtr, this);
  }

  public void setReserve1(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_reserve1_set(swigCPtr, this, value);
  }

  public String getReserve1() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_reserve1_get(swigCPtr, this);
  }

  public void setDirection(char value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_Direction_set(swigCPtr, this, value);
  }

  public char getDirection() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_Direction_get(swigCPtr, this);
  }

  public void setInstrumentID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryOptionInstrTradingRightField_InstrumentID_get(swigCPtr, this);
  }

  public CThostFtdcQryOptionInstrTradingRightField() {
    this(jctpv6v5v1cpx64apiJNI.new_CThostFtdcQryOptionInstrTradingRightField(), true);
  }

}
