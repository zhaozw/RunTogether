package cn.bmob.imdemo.ui.fragment.connectfragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.bmob.imdemo.R;

public class ConnectPeopleFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {


		View rootView =inflater.inflate(R.layout.fragment_connect_people, container, false);
		return rootView;
	}
}
