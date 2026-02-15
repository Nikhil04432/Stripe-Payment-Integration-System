package com.hulkhiretech.payments.service.interfaces;

import com.hulkhiretech.payments.pojo.NotificationRequest;

public interface NotificationService {
	
	public void processNotification(NotificationRequest notificationRequest);

}
