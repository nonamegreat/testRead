/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v5v1cpv.api;

public class CThostFtdcSyncDelaySwapFrozenField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcSyncDelaySwapFrozenField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcSyncDelaySwapFrozenField obj) {
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
        jctpv6v5v1cpx64apiJNI.delete_CThostFtdcSyncDelaySwapFrozenField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setDelaySwapSeqNo(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_DelaySwapSeqNo_set(swigCPtr, this, value);
  }

  public String getDelaySwapSeqNo() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_DelaySwapSeqNo_get(swigCPtr, this);
  }

  public void setBrokerID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_InvestorID_get(swigCPtr, this);
  }

  public void setFromCurrencyID(String value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_FromCurrencyID_set(swigCPtr, this, value);
  }

  public String getFromCurrencyID() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_FromCurrencyID_get(swigCPtr, this);
  }

  public void setFromRemainSwap(double value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_FromRemainSwap_set(swigCPtr, this, value);
  }

  public double getFromRemainSwap() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_FromRemainSwap_get(swigCPtr, this);
  }

  public void setIsManualSwap(int value) {
    jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_IsManualSwap_set(swigCPtr, this, value);
  }

  public int getIsManualSwap() {
    return jctpv6v5v1cpx64apiJNI.CThostFtdcSyncDelaySwapFrozenField_IsManualSwap_get(swigCPtr, this);
  }

  public CThostFtdcSyncDelaySwapFrozenField() {
    this(jctpv6v5v1cpx64apiJNI.new_CThostFtdcSyncDelaySwapFrozenField(), true);
  }

}
