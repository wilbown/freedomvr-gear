/************************************************************************************

Filename    :   OVR_Win32_DeviceManager.h
Content     :   Win32-specific DeviceManager header.
Created     :   September 21, 2012
Authors     :   Michael Antonov

Copyright   :   Copyright 2014 Oculus VR, LLC. All Rights reserved.

*************************************************************************************/

#ifndef OVR_Win32_DeviceManager_h
#define OVR_Win32_DeviceManager_h

#include "OVR_DeviceImpl.h"
#include "OVR_Win32_DeviceStatus.h"

#include "LibOVR/Src/Kernel/OVR_Timer.h"


namespace OVR { namespace Win32 {

class DeviceManagerThread;

//-------------------------------------------------------------------------------------
// ***** Win32 DeviceManager

class DeviceManager : public DeviceManagerImpl
{
public:
    DeviceManager();
    ~DeviceManager();

    // Initialize/Shutdowncreate and shutdown manger thread.
    virtual bool Initialize(DeviceBase* parent);
    virtual void Shutdown();

    virtual ThreadCommandQueue* GetThreadQueue();
    virtual ThreadId GetThreadId() const;
    virtual int GetThreadTid() const;
    virtual void SuspendThread() const;
    virtual void ResumeThread() const;

    virtual DeviceEnumerator<> EnumerateDevicesEx(const DeviceEnumerationArgs& args);    

    virtual bool  GetDeviceInfo(DeviceInfo* info) const;

    // Fills HIDDeviceDesc by using the path.
    // Returns 'true' if successful, 'false' otherwise.
    bool GetHIDDeviceDesc(const String& path, HIDDeviceDesc* pdevDesc) const;
    
    Ptr<DeviceManagerThread> pThread;
};

//-------------------------------------------------------------------------------------
// ***** Device Manager Background Thread

class DeviceManagerThread : public Thread, public ThreadCommandQueue, public DeviceStatus::Notifier
{
    friend class DeviceManager;
    enum { ThreadStackSize = 32 * 1024 };
public:
    DeviceManagerThread(DeviceManager* pdevMgr);
    ~DeviceManagerThread();

    virtual int Run();

    // ThreadCommandQueue notifications for CommandEvent handling.
    virtual void OnPushNonEmpty_Locked() { ::SetEvent(hCommandEvent); }
    virtual void OnPopEmpty_Locked()     { ::ResetEvent(hCommandEvent); }


    // Notifier used for different updates (EVENT or regular timing or messages).
    class Notifier
    {
    public:
		// Called when overlapped I/O handle is signaled.
        virtual void    OnOverlappedEvent(HANDLE hevent) { OVR_UNUSED1(hevent); }

        // Called when timing ticks are updated.
        // Returns the largest number of seconds this function can
        // wait till next call.
        virtual double  OnTicks(double tickSeconds)
        { OVR_UNUSED1(tickSeconds);  return 1000.0; }

		enum DeviceMessageType
		{
			DeviceMessage_DeviceAdded     = 0,
			DeviceMessage_DeviceRemoved   = 1,
		};

		// Called to notify device object.
		virtual bool    OnDeviceMessage(DeviceMessageType messageType, 
										const String& devicePath,
										bool* error) 
        { OVR_UNUSED3(messageType, devicePath, error); return false; }
    };

 
    // Adds device's OVERLAPPED structure for I/O.
    // After it's added, Overlapped object will be signaled if a message arrives.
    bool AddOverlappedEvent(Notifier* notify, HANDLE hevent);
    bool RemoveOverlappedEvent(Notifier* notify, HANDLE hevent);

    // Add notifier that will be called at regular intervals. 
    bool AddTicksNotifier(Notifier* notify);
    bool RemoveTicksNotifier(Notifier* notify);

	bool AddMessageNotifier(Notifier* notify);
	bool RemoveMessageNotifier(Notifier* notify);

    // DeviceStatus::Notifier interface.
	bool OnMessage(MessageType type, const String& devicePath);

    void DetachDeviceManager();

private:
    bool threadInitialized() { return hCommandEvent != 0; }

    // Event used to wake us up thread commands are enqueued.    
    HANDLE                  hCommandEvent;

    // Event notifications for devices whose OVERLAPPED I/O we service.
    // This list is modified through AddDeviceOverlappedEvent.
    // WaitHandles[0] always == hCommandEvent, with null device.
    ArrayPOD<HANDLE>        WaitHandles;
    ArrayPOD<Notifier*>     WaitNotifiers;

    // Ticks notifiers - used for time-dependent events such as keep-alive.
    ArrayPOD<Notifier*>     TicksNotifiers;

	// Message notifiers.
    ArrayPOD<Notifier*>     MessageNotifiers;

	// Object that manages notifications originating from Windows messages.
	Ptr<DeviceStatus>		pStatusObject;

    Lock                    DevMgrLock;
    // pDeviceMgr should be accessed under DevMgrLock
    DeviceManager*          pDeviceMgr; // back ptr, no addref. 
};

}} // namespace Win32::OVR

#endif // OVR_Win32_DeviceManager_h
