/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package xyz.redtorch.gateway.ctp.x64v6v3v19t1v.api;

public class CThostFtdcDRTransferField {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected CThostFtdcDRTransferField(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(CThostFtdcDRTransferField obj) {
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
        jctpv6v3v19t1x64apiJNI.delete_CThostFtdcDRTransferField(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setOrigDRIdentityID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_OrigDRIdentityID_set(swigCPtr, this, value);
  }

  public int getOrigDRIdentityID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_OrigDRIdentityID_get(swigCPtr, this);
  }

  public void setDestDRIdentityID(int value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_DestDRIdentityID_set(swigCPtr, this, value);
  }

  public int getDestDRIdentityID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_DestDRIdentityID_get(swigCPtr, this);
  }

  public void setOrigBrokerID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_OrigBrokerID_set(swigCPtr, this, value);
  }

  public String getOrigBrokerID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_OrigBrokerID_get(swigCPtr, this);
  }

  public void setDestBrokerID(String value) {
    jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_DestBrokerID_set(swigCPtr, this, value);
  }

  public String getDestBrokerID() {
    return jctpv6v3v19t1x64apiJNI.CThostFtdcDRTransferField_DestBrokerID_get(swigCPtr, this);
  }

  public CThostFtdcDRTransferField() {
    this(jctpv6v3v19t1x64apiJNI.new_CThostFtdcDRTransferField(), true);
  }

}
