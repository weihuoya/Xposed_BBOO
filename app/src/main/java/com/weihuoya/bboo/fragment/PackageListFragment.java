package com.weihuoya.bboo.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.weihuoya.bboo.GridDividerDecoration;
import com.weihuoya.bboo.R;
import com.weihuoya.bboo._G;
import com.weihuoya.bboo._P;
import com.weihuoya.bboo.activity.AppDetailActivity;
import com.weihuoya.bboo.model.PackageModel;
import com.weihuoya.bboo.widget.MyToggleButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PackageListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PackageListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PackageListFragment extends Fragment {

    public class PackageViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener, MyToggleButton.OnCheckedChangeListener {
        @Bind(R.id.app_icon)
        protected ImageView appIcon;
        @Bind(R.id.app_name)
        protected TextView appName;
        @Bind(R.id.app_package)
        protected TextView appPackage;
        @Bind(R.id.app_desc)
        protected TextView appDesc;
        @Bind(R.id.app_status_toggle)
        protected MyToggleButton toggleButton;

        public PackageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
            //itemView.setOnLongClickListener(this);
        }

        @Override
        public void onCheckedChanged(MyToggleButton buttonView, boolean isChecked) {
            if(_P.blockPackage(getPackageName(), isChecked)) {
                if(isChecked) {
                    itemView.setBackgroundColor(_G.getColor(R.color.colorActionDisable));
                } else {
                    itemView.setBackgroundColor(_G.getColor(android.R.color.white));
                }
            } else {
                buttonView.setChecked(!isChecked, true);
            }
        }

        @Override
        public void onClick(View v) {
            _P.showPackageDetail(getPackageName());
        }

        @Override
        public boolean onLongClick(View v) {
            Intent intent = _G.getPackageManager().getLaunchIntentForPackage(getPackageName());
            if(intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _G.getContext().startActivity(intent);
                return true;
            }
            return false;
        }

        public String getPackageName() {
            return appPackage.getText().toString();
        }

        public void bindModel(final PackageModel model) {
            appIcon.setImageDrawable(model.getIcon());
            appName.setText(model.getName());
            appPackage.setText(model.getPackageName());
            itemView.setBackgroundColor(_G.getColor(android.R.color.white));

            appIcon.setOnClickListener(this);
            appIcon.setOnLongClickListener(this);

            if(mParam1 == R.string.main_tab_prc) {
                toggleButton.setVisibility(View.GONE);
                appDesc.setText(model.getProcessDescription());
            } else {
                boolean isBlocked = model.isBlocked() || !model.isApplicationEnabled();
                toggleButton.setVisibility(View.VISIBLE);
                toggleButton.setOnCheckedChangeListener(null);
                toggleButton.setChecked(isBlocked, false);
                toggleButton.setOnCheckedChangeListener(this);
                if(isBlocked) {
                    itemView.setBackgroundColor(_G.getColor(R.color.colorActionDisable));
                }
                appDesc.setText(model.getPackageDescription());
            }
        }
    }

    public class PackageListAdapter extends RecyclerView.Adapter<PackageViewHolder> {
        private List<PackageModel> mDataset;

        public PackageListAdapter() {
            mDataset = new ArrayList<>();
        }

        @Override
        public PackageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            _G.log(":: onCreateViewHolder");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
            return new PackageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PackageViewHolder holder, int position) {
            _G.log(":: onBindViewHolder");
            holder.bindModel(mDataset.get(position));
        }

        @Override
        public int getItemCount() {
            _G.log(":: getItemCount");
            return mDataset.size();
        }

        @Override
        public void onViewAttachedToWindow(PackageViewHolder holder) {
            _G.log(":: onViewAttachedToWindow");
        }

        @Override
        public void onViewDetachedFromWindow(PackageViewHolder holder) {
            _G.log(":: onViewDetachedFromWindow");
        }

        public void setDataset(final List<PackageModel> dataset) {
            mDataset = dataset;
            notifyDataSetChanged();
        }
    }

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private int mParam1;

    private String mQuery;


    private OnFragmentInteractionListener mListener;

    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    public PackageListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment PackageListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PackageListFragment newInstance(int param1) {
        PackageListFragment fragment = new PackageListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getInt(ARG_PARAM1);
            mQuery = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_package_list, container, false);

        GridDividerDecoration decoration = new GridDividerDecoration(
                getContext().getDrawable(R.drawable.line_divider), true, false
        );

        mRecyclerView = (RecyclerView)view.findViewById(R.id.package_list_view);
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(new PackageListAdapter());

        mProgressBar = (ProgressBar)view.findViewById(R.id.load_progress_bar);

        return view;
    }

    public void setQuery(String query) {
        int queryLength = 0;
        List<PackageModel> dataset = new ArrayList<>();
        RecyclerView.Adapter<?> adapter = mRecyclerView != null ? mRecyclerView.getAdapter() : null;

        if(adapter == null || (mParam1 == R.string.main_tab_prc && _P.ProcessList == null) || _P.PackageList == null) {
            return;
        }

        mProgressBar.setVisibility(View.GONE);

        if(query != null) {
            queryLength = TextUtils.getTrimmedLength(query);
            query = query.toLowerCase();
        }

        mQuery = query;

        if(mParam1 == R.string.main_tab_prc) {
            for(PackageModel model : _P.ProcessList) {
                if(queryLength == 0 ||
                        model.getName().toLowerCase().contains(query) ||
                        model.getPackageName().toLowerCase().contains(query)) {
                    dataset.add(model);
                }
            }
        } else {
            for(PackageModel model : _P.PackageList) {
                if(queryLength == 0 ||
                        model.getName().toLowerCase().contains(query) ||
                        model.getPackageName().toLowerCase().contains(query)) {
                    if(model.isSystemApplication()) {
                        if(mParam1 == R.string.main_tab_sys) {
                            dataset.add(model);
                        }
                    } else if(mParam1 == R.string.main_tab_usr) {
                        dataset.add(model);
                    }
                }
            }
        }

        ((PackageListAdapter)adapter).setDataset(dataset);
    }

    public void scrollToPosition(int position, boolean smooth) {
        if(smooth) {
            mRecyclerView.smoothScrollToPosition(position);
        } else {
            mRecyclerView.scrollToPosition(position);
        }
    }

    public void handlePackageChanged(String packageName) {
        setQuery(mQuery);
    }

    public void handlePackageRemoved(String packageName) {
        setQuery(mQuery);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
            setQuery(mQuery);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
