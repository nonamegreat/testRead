/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v5v1cpv.api;

public class CThostFtdcQryClassifiedInstrumentField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcQryClassifiedInstrumentField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcQryClassifiedInstrumentField obj) {
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
        jctpv6v5v1cpx64apiJNI.delete_CThostFtdcQryClassifiedInstrumentField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setInstrumentID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_InstrumentID_get(swigCPtr, this);
  }

  public void setExchangeID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ExchangeID_set(swigCPtr, this, value);
  }

  public String getExchangeID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ExchangeID_get(swigCPtr, this);
  }

  public void setExchangeInstID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ExchangeInstID_set(swigCPtr, this, value);
  }

  public String getExchangeInstID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ExchangeInstID_get(swigCPtr, this);
  }

  public void setProductID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ProductID_set(swigCPtr, this, value);
  }

  public String getProductID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ProductID_get(swigCPtr, this);
  }

  public void setTradingType(char value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_TradingType_set(swigCPtr, this, value);
  }

  public char getTradingType() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_TradingType_get(swigCPtr, this);
  }

  public void setClassType(char value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ClassType_set(swigCPtr, this, value);
  }

  public char getClassType() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcQryClassifiedInstrumentField_ClassType_get(swigCPtr, this);
  }

  public CThostFtdcQryClassifiedInstrumentField() {
    this(jctpv6v5v1cpx64apiJNI.new_CThostFtdcQryClassifiedInstrumentField(), true);
  }

}