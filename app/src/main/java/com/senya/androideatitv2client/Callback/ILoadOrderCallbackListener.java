package com.senya.androideatitv2client.Callback;

import com.senya.androideatitv2client.Model.Order;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<Order> orderList);
    void onLoadOrderFailed(String message);
}
