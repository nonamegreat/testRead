/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v5v1v.api;

public class CThostFtdcNotifyFutureSignOutField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcNotifyFutureSignOutField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcNotifyFutureSignOutField obj) {
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
        jctpv6v5v1x64apiJNI.delete_CThostFtdcNotifyFutureSignOutField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setTradeCode(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradeCode_set(swigCPtr, this, value);
  }

  public String getTradeCode() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradeCode_get(swigCPtr, this);
  }

  public void setBankID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BankID_set(swigCPtr, this, value);
  }

  public String getBankID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BankID_get(swigCPtr, this);
  }

  public void setBankBranchID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BankBranchID_set(swigCPtr, this, value);
  }

  public String getBankBranchID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BankBranchID_get(swigCPtr, this);
  }

  public void setBrokerID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BrokerID_set(swigCPtr, this, value);
  }

  public String getBrokerID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BrokerID_get(swigCPtr, this);
  }

  public void setBrokerBranchID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BrokerBranchID_set(swigCPtr, this, value);
  }

  public String getBrokerBranchID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BrokerBranchID_get(swigCPtr, this);
  }

  public void setTradeDate(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradeDate_set(swigCPtr, this, value);
  }

  public String getTradeDate() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradeDate_get(swigCPtr, this);
  }

  public void setTradeTime(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradeTime_set(swigCPtr, this, value);
  }

  public String getTradeTime() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradeTime_get(swigCPtr, this);
  }

  public void setBankSerial(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BankSerial_set(swigCPtr, this, value);
  }

  public String getBankSerial() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BankSerial_get(swigCPtr, this);
  }

  public void setTradingDay(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradingDay_set(swigCPtr, this, value);
  }

  public String getTradingDay() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TradingDay_get(swigCPtr, this);
  }

  public void setPlateSerial(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_PlateSerial_set(swigCPtr, this, value);
  }

  public int getPlateSerial() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_PlateSerial_get(swigCPtr, this);
  }

  public void setLastFragment(char value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_LastFragment_set(swigCPtr, this, value);
  }

  public char getLastFragment() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_LastFragment_get(swigCPtr, this);
  }

  public void setSessionID(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_SessionID_set(swigCPtr, this, value);
  }

  public int getSessionID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_SessionID_get(swigCPtr, this);
  }

  public void setInstallID(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_InstallID_set(swigCPtr, this, value);
  }

  public int getInstallID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_InstallID_get(swigCPtr, this);
  }

  public void setUserID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_UserID_set(swigCPtr, this, value);
  }

  public String getUserID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_UserID_get(swigCPtr, this);
  }

  public void setDigest(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_Digest_set(swigCPtr, this, value);
  }

  public String getDigest() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_Digest_get(swigCPtr, this);
  }

  public void setCurrencyID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_CurrencyID_set(swigCPtr, this, value);
  }

  public String getCurrencyID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_CurrencyID_get(swigCPtr, this);
  }

  public void setDeviceID(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_DeviceID_set(swigCPtr, this, value);
  }

  public String getDeviceID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_DeviceID_get(swigCPtr, this);
  }

  public void setBrokerIDByBank(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BrokerIDByBank_set(swigCPtr, this, value);
  }

  public String getBrokerIDByBank() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_BrokerIDByBank_get(swigCPtr, this);
  }

  public void setOperNo(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_OperNo_set(swigCPtr, this, value);
  }

  public String getOperNo() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_OperNo_get(swigCPtr, this);
  }

  public void setRequestID(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_RequestID_set(swigCPtr, this, value);
  }

  public int getRequestID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_RequestID_get(swigCPtr, this);
  }

  public void setTID(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TID_set(swigCPtr, this, value);
  }

  public int getTID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_TID_get(swigCPtr, this);
  }

  public void setErrorID(int value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_ErrorID_set(swigCPtr, this, value);
  }

  public int getErrorID() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_ErrorID_get(swigCPtr, this);
  }

  public void setErrorMsg(String value) {
    jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_ErrorMsg_set(swigCPtr, this, value);
  }

  public String getErrorMsg() {
    return jctpv6v5v1x64apiJNI.CThostFtdcNotifyFutureSignOutField_ErrorMsg_get(swigCPtr, this);
  }

  public CThostFtdcNotifyFutureSignOutField() {
    this(jctpv6v5v1x64apiJNI.new_CThostFtdcNotifyFutureSignOutField(), true);
  }

}
