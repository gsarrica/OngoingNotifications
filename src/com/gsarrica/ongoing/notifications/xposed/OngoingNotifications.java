package com.gsarrica.ongoing.notifications.xposed;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.app.Notification;
import android.os.Build;

public class OngoingNotifications implements IXposedHookZygoteInit {
	final int sdk = Build.VERSION.SDK_INT;
	List<String> whiteList = new ArrayList<String>();
	
	public void hookNotifications()  {
		XC_MethodHook notifyHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

				String packageName = (String) param.args[0];
				XposedBridge.log("Notification from " + packageName);

				//Cast the Notification to read flags and check for onGoing state
				Notification notification;
				if (sdk <= 15 || sdk >= 18)
					notification = (Notification) param.args[6];
				else
					notification = (Notification) param.args[5];
				Boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;

				if(isOngoing) {
					//setResult flags this method hook to return early. As this is a void method we set the result to null as it is unused.
					XposedBridge.log("Block ongoing noti from " + packageName);
					if(!whiteList.contains(packageName)) {
						param.setResult(null);
					}
				}

			}
		};

		//Hook for various SDK versions
		switch(sdk) {
			case 15:
				XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "enqueueNotificationInternal", String.class, int.class, int.class,
						String.class, int.class, int.class, Notification.class, int[].class,
						notifyHook);
				break;
			case 16: 
				XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "enqueueNotificationInternal", String.class, int.class, int.class,
						String.class, int.class, Notification.class, int[].class,
						notifyHook);
				break;
			case 17:
				XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "enqueueNotificationInternal", String.class, int.class, int.class,
						String.class, int.class, Notification.class, int[].class, int.class,
						notifyHook);
				break;
			case 18:
				XposedHelpers.findAndHookMethod("com.android.server.NotificationManagerService", null, "enqueueNotificationInternal", String.class, String.class,
						int.class, int.class, String.class, int.class, Notification.class, int[].class, int.class,
						notifyHook);
				break;
			default: //fail
				break;
		}
	}
	


	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		XSharedPreferences pref = new XSharedPreferences(OngoingNotifications.class.getPackage().getName());
		whiteList.add("com.android.phone");
		try {
			hookNotifications();
			XposedBridge.log("Hiding ongoing notifications");

		}
		catch (Throwable t) {
			XposedBridge.log("Cant hide ongoing notifications (could not hook method)");
			XposedBridge.log(t);
		}
				
		
	}
}
