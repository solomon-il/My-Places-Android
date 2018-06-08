package com.shlominet.utils;

import java.util.ArrayList;
import java.util.List;

//helper interfaces
public class MyInterface {

    public interface OnItemClickListener {
        void onItemClick(MapPointObj item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(MapPointObj item);
    }

    public interface FragHelper {

        void passDataL2M(MapPointObj pointObj, ArrayList<MapPointObj> pointObjList);
        void passDataL2M(MapPointObj pointObj);

        void getGpsLocation();
    }
}
