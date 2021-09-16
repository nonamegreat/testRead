/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v3v19t1v.api;

public class CThostFtdcOptionSelfCloseField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcOptionSelfCloseField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcOptionSelfCloseField obj) {
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
        jctpv6v3v19t1x64apiJNI.delete_CThostFtdcOptionSelfCloseField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setBrokerID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BrokerID_get(swigCPtr, this);
  }

  public void setInvestorID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InvestorID_set(swigCPtr, this, value);
  }

  public String getInvestorID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InvestorID_get(swigCPtr, this);
  }

  public void setInstrumentID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InstrumentID_set(swigCPtr, this, value);
  }

  public String getInstrumentID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InstrumentID_get(swigCPtr, this);
  }

  public void setOptionSelfCloseRef(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptionSelfCloseRef_set(swigCPtr, this, value);
  }

  public String getOptionSelfCloseRef() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptionSelfCloseRef_get(swigCPtr, this);
  }

  public void setUserID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_UserID_set(swigCPtr, this, value);
  }

  public String getUserID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_UserID_get(swigCPtr, this);
  }

  public void setVolume(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_Volume_set(swigCPtr, this, value);
  }

  public int getVolume() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_Volume_get(swigCPtr, this);
  }

  public void setRequestID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_RequestID_set(swigCPtr, this, value);
  }

  public int getRequestID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_RequestID_get(swigCPtr, this);
  }

  public void setBusinessUnit(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BusinessUnit_set(swigCPtr, this, value);
  }

  public String getBusinessUnit() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BusinessUnit_get(swigCPtr, this);
  }

  public void setHedgeFlag(char value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_HedgeFlag_set(swigCPtr, this, value);
  }

  public char getHedgeFlag() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_HedgeFlag_get(swigCPtr, this);
  }

  public void setOptSelfCloseFlag(char value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptSelfCloseFlag_set(swigCPtr, this, value);
  }

  public char getOptSelfCloseFlag() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptSelfCloseFlag_get(swigCPtr, this);
  }

  public void setOptionSelfCloseLocalID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptionSelfCloseLocalID_set(swigCPtr, this, value);
  }

  public String getOptionSelfCloseLocalID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptionSelfCloseLocalID_get(swigCPtr, this);
  }

  public void setExchangeID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ExchangeID_set(swigCPtr, this, value);
  }

  public String getExchangeID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ExchangeID_get(swigCPtr, this);
  }

  public void setParticipantID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ParticipantID_set(swigCPtr, this, value);
  }

  public String getParticipantID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ParticipantID_get(swigCPtr, this);
  }

  public void setClientID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ClientID_set(swigCPtr, this, value);
  }

  public String getClientID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ClientID_get(swigCPtr, this);
  }

  public void setExchangeInstID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ExchangeInstID_set(swigCPtr, this, value);
  }

  public String getExchangeInstID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ExchangeInstID_get(swigCPtr, this);
  }

  public void setTraderID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_TraderID_set(swigCPtr, this, value);
  }

  public String getTraderID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_TraderID_get(swigCPtr, this);
  }

  public void setInstallID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InstallID_set(swigCPtr, this, value);
  }

  public int getInstallID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InstallID_get(swigCPtr, this);
  }

  public void setOrderSubmitStatus(char value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OrderSubmitStatus_set(swigCPtr, this, value);
  }

  public char getOrderSubmitStatus() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OrderSubmitStatus_get(swigCPtr, this);
  }

  public void setNotifySequence(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_NotifySequence_set(swigCPtr, this, value);
  }

  public int getNotifySequence() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_NotifySequence_get(swigCPtr, this);
  }

  public void setTradingDay(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_TradingDay_set(swigCPtr, this, value);
  }

  public String getTradingDay() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_TradingDay_get(swigCPtr, this);
  }

  public void setSettlementID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_SettlementID_set(swigCPtr, this, value);
  }

  public int getSettlementID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_SettlementID_get(swigCPtr, this);
  }

  public void setOptionSelfCloseSysID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptionSelfCloseSysID_set(swigCPtr, this, value);
  }

  public String getOptionSelfCloseSysID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_OptionSelfCloseSysID_get(swigCPtr, this);
  }

  public void setInsertDate(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InsertDate_set(swigCPtr, this, value);
  }

  public String getInsertDate() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InsertDate_get(swigCPtr, this);
  }

  public void setInsertTime(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InsertTime_set(swigCPtr, this, value);
  }

  public String getInsertTime() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InsertTime_get(swigCPtr, this);
  }

  public void setCancelTime(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_CancelTime_set(swigCPtr, this, value);
  }

  public String getCancelTime() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_CancelTime_get(swigCPtr, this);
  }

  public void setExecResult(char value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ExecResult_set(swigCPtr, this, value);
  }

  public char getExecResult() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ExecResult_get(swigCPtr, this);
  }

  public void setClearingPartID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ClearingPartID_set(swigCPtr, this, value);
  }

  public String getClearingPartID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ClearingPartID_get(swigCPtr, this);
  }

  public void setSequenceNo(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_SequenceNo_set(swigCPtr, this, value);
  }

  public int getSequenceNo() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_SequenceNo_get(swigCPtr, this);
  }

  public void setFrontID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_FrontID_set(swigCPtr, this, value);
  }

  public int getFrontID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_FrontID_get(swigCPtr, this);
  }

  public void setSessionID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_SessionID_set(swigCPtr, this, value);
  }

  public int getSessionID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_SessionID_get(swigCPtr, this);
  }

  public void setUserProductInfo(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_UserProductInfo_set(swigCPtr, this, value);
  }

  public String getUserProductInfo() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_UserProductInfo_get(swigCPtr, this);
  }

  public void setStatusMsg(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_StatusMsg_set(swigCPtr, this, value);
  }

  public String getStatusMsg() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_StatusMsg_get(swigCPtr, this);
  }

  public void setActiveUserID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ActiveUserID_set(swigCPtr, this, value);
  }

  public String getActiveUserID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_ActiveUserID_get(swigCPtr, this);
  }

  public void setBrokerOptionSelfCloseSeq(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BrokerOptionSelfCloseSeq_set(swigCPtr, this, value);
  }

  public int getBrokerOptionSelfCloseSeq() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BrokerOptionSelfCloseSeq_get(swigCPtr, this);
  }

  public void setBranchID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BranchID_set(swigCPtr, this, value);
  }

  public String getBranchID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_BranchID_get(swigCPtr, this);
  }

  public void setInvestUnitID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InvestUnitID_set(swigCPtr, this, value);
  }

  public String getInvestUnitID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_InvestUnitID_get(swigCPtr, this);
  }

  public void setAccountID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_AccountID_set(swigCPtr, this, value);
  }

  public String getAccountID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_AccountID_get(swigCPtr, this);
  }

  public void setCurrencyID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_CurrencyID_set(swigCPtr, this, value);
  }

  public String getCurrencyID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_CurrencyID_get(swigCPtr, this);
  }

  public void setIPAddress(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_IPAddress_set(swigCPtr, this, value);
  }

  public String getIPAddress() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_IPAddress_get(swigCPtr, this);
  }

  public void setMacAddress(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_MacAddress_set(swigCPtr, this, value);
  }

  public String getMacAddress() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcOptionSelfCloseField_MacAddress_get(swigCPtr, this);
  }

  public CThostFtdcOptionSelfCloseField() {
    this(jctpv6v3v19t1x64apiJNI.new_CThostFtdcOptionSelfCloseField(), true);
  }

}
