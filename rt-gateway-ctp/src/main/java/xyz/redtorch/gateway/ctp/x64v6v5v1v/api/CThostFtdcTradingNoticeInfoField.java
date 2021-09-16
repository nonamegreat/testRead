/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v5v1v.api;

public class CThostFtdcTradingNoticeInfoField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcTradingNoticeInfoField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcTradingNoticeInfoField obj) {
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
        jctpv6v5v1x64apiJNI.delete_CThostFtdcTradingNoticeInfoField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_InvestorID_get(swigCPtr, this);
  }

  public void setSendTime(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_SendTime_set(swigCPtr, this, value);
  }

  public String getSendTime() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_SendTime_get(swigCPtr, this);
  }

  public void setFieldContent(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_FieldContent_set(swigCPtr, this, value);
  }

  public String getFieldContent() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_FieldContent_get(swigCPtr, this);
  }

  public void setSequenceSeries(short value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_SequenceSeries_set(swigCPtr, this, value);
  }

  public short getSequenceSeries() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_SequenceSeries_get(swigCPtr, this);
  }

  public void setSequenceNo(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_SequenceNo_set(swigCPtr, this, value);
  }

  public int getSequenceNo() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_SequenceNo_get(swigCPtr, this);
  }

  public void setInvestUnitID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_InvestUnitID_set(swigCPtr, this, value);
  }

  public String getInvestUnitID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcTradingNoticeInfoField_InvestUnitID_get(swigCPtr, this);
  }

  public CThostFtdcTradingNoticeInfoField() {
    this(jctpv6v5v1x64apiJNI.new_CThostFtdcTradingNoticeInfoField(), true);
  }

}
