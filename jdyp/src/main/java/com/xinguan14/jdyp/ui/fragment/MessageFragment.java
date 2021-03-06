package com.xinguan14.jdyp.ui.fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.xinguan14.jdyp.myVeiw.CircleImageView;
import com.xinguan14.jdyp.R;
import com.xinguan14.jdyp.swipMenu.SwapRecyclerView;
import com.xinguan14.jdyp.swipMenu.SwipeMenu;
import com.xinguan14.jdyp.swipMenu.SwipeMenuBuilder;
import com.xinguan14.jdyp.swipMenu.SwipeMenuItem;
import com.xinguan14.jdyp.swipMenu.SwipeMenuView;
import com.xinguan14.jdyp.adapter.OnRecyclerViewListener;
import com.xinguan14.jdyp.adapter.base.BaseRecyclerAdapter;
import com.xinguan14.jdyp.adapter.base.BaseRecyclerHolder;
import com.xinguan14.jdyp.adapter.base.IMutlipleItem;
import com.xinguan14.jdyp.base.ParentWithNaviActivity;
import com.xinguan14.jdyp.base.ParentWithNaviFragment;
import com.xinguan14.jdyp.bean.Conversation;
import com.xinguan14.jdyp.bean.PrivateConversation;
import com.xinguan14.jdyp.event.RefreshEvent;
import com.xinguan14.jdyp.reddot.DisplayUtils;
import com.xinguan14.jdyp.reddot.StickyViewHelper;
import com.xinguan14.jdyp.ui.SearchUserActivity;
import com.xinguan14.jdyp.util.ImageLoadOptions;
import com.xinguan14.jdyp.util.TimeUtil;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.bmob.newim.BmobIM;
import cn.bmob.newim.bean.BmobIMConversation;
import cn.bmob.newim.db.BmobIMDBManager;
import cn.bmob.newim.event.MessageEvent;
import cn.bmob.newim.event.OfflineMessageEvent;

/**
 * 会话界面
 *
 * @author :smile
 * @project:ConversationFragment
 * @date :2016-01-25-18:23
 */
public class MessageFragment extends ParentWithNaviFragment implements SwipeMenuBuilder {

    @Bind(R.id.rc_view)
    SwapRecyclerView rc_view;

    @Bind(R.id.no_message)
    FrameLayout noMessage;
    public ConversationAdapter adapter;
    LinearLayoutManager layoutManager;
    SwipeMenuView menuView;//包含左滑删除的布局

    @Override
    protected String title() {
        return "消息";
    }

    @Override
    public Object right() {
        return R.drawable.base_action_bar_add_bg_selector;
    }

    @Override
    public ParentWithNaviActivity.ToolBarListener setToolBarListener() {
        return new ParentWithNaviActivity.ToolBarListener() {
            @Override
            public void clickLeft() {

            }

            @Override
            public void clickRight() {
                startActivity(SearchUserActivity.class, null);
            }
        };
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_conversation, container, false);
        initNaviView();
        ButterKnife.bind(this, rootView);
        //单一布局
        IMutlipleItem<Conversation> mutlipleItem = new IMutlipleItem<Conversation>() {

            @Override
            public int getItemViewType(int postion, Conversation c) {
                return 0;
            }

            @Override
            public int getItemLayoutId(int viewtype) {
                return R.layout.item_conversation;
            }

            @Override
            public int getItemCount(List<Conversation> list) {
                return list.size();
            }
        };

        adapter = new ConversationAdapter(getActivity(), mutlipleItem, null, this);

        rc_view.setItemAnimator(new DefaultItemAnimator());
        rc_view.setAdapter(adapter);

        layoutManager = new LinearLayoutManager(getActivity());
        rc_view.setLayoutManager(layoutManager);

        setListener();
        showNoMessage();
        return rootView;
    }

    private void setListener() {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                query();
            }
        });
        adapter.setOnRecyclerViewListener(new OnRecyclerViewListener() {
            @Override
            public void onItemClick(int position) {
                adapter.getItem(position).onClick(getActivity());
            }

            @Override
            public boolean onItemLongClick(int position) {
                adapter.getItem(position).onLongClick(getActivity());
                adapter.remove(position);
                showNoMessage();
                return true;
            }
        });
    }

    private void showNoMessage() {
        if (getConversations().size() == 0) {
            noMessage.setVisibility(View.VISIBLE);
        } else {
            noMessage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        query();
        showNoMessage();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (menuView != null) {
            if (hidden == true && menuView.getLayout().isOpen()) {//当消息列表界面被隐藏掉时，把打开的左滑删除按钮关闭
                menuView.getLayout().smoothCloseMenu();
            }
        }
    }

    /**
     * 查询本地会话
     */
    public void query() {
//        adapter.bindDatas(BmobIM.getInstance().loadAllConversation());
        adapter.bindDatas(getConversations());
        adapter.notifyDataSetChanged();
    }

    /**
     * 获取会话列表的数据：增加新朋友会话
     *
     * @return
     */
    private List<Conversation> getConversations() {
        //添加会话
        List<Conversation> conversationList = new ArrayList<>();
        conversationList.clear();
        List<BmobIMConversation> list = BmobIM.getInstance().loadAllConversation();
        if (list != null && list.size() > 0) {
            for (BmobIMConversation item : list) {
                switch (item.getConversationType()) {
                    case 1://私聊
                        conversationList.add(new PrivateConversation(item));
                        break;
                    default:
                        break;
                }
            }
        }
        //重新排序
        Collections.sort(conversationList);
        return conversationList;
    }

    /**
     * 注册自定义消息接收事件
     *
     * @param event
     */
    @Subscribe
    public void onEventMainThread(RefreshEvent event) {
        log("---会话页接收到自定义消息---");
        //因为新增`新朋友`这种会话类型
        adapter.bindDatas(getConversations());
        adapter.notifyDataSetChanged();
    }

    /**
     * 注册离线消息接收事件
     *
     * @param event
     */
    @Subscribe
    public void onEventMainThread(OfflineMessageEvent event) {
        //重新刷新列表
        adapter.bindDatas(getConversations());
        adapter.notifyDataSetChanged();
    }

    /**
     * 注册消息接收事件
     *
     * @param event 1、与用户相关的由开发者自己维护，SDK内部只存储用户信息
     *              2、开发者获取到信息后，可调用SDK内部提供的方法更新会话
     */
    @Subscribe
    public void onEventMainThread(MessageEvent event) {
        //重新获取本地消息并刷新列表
        adapter.bindDatas(getConversations());
        adapter.notifyDataSetChanged();
    }

    @Override
    public SwipeMenuView create() {
        SwipeMenu menu = new SwipeMenu(getActivity());

        SwipeMenuItem item = new SwipeMenuItem(getActivity());
        item.setTitle("删除")
                .setTitleColor(Color.WHITE)
                .setBackground(new ColorDrawable(Color.RED));
        menu.addMenuItem(item);

        menuView = new SwipeMenuView(menu);

        menuView.setOnMenuItemClickListener(new SwipeMenuView.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(int position, SwipeMenu menu, int index) {
                rc_view.smoothCloseMenu(position);
                adapter.getItem(position).onLongClick(getActivity());
                adapter.remove(position);
                showNoMessage();
            }
        });

        return menuView;
    }

    /**
     * 使用进一步封装的Conversation,教大家怎么自定义会话列表
     *
     * @author smile
     */
    class ConversationAdapter extends BaseRecyclerAdapter<Conversation> {

        /**
         * 用于记录需要删除的view的position
         */
        ArrayList<Integer> removeList = new ArrayList<Integer>();
        protected final Context context;

        public ConversationAdapter(Context context, IMutlipleItem<Conversation> items, Collection<Conversation> datas, Fragment fragment) {
            super(context, items, datas, fragment);
            this.context = context;

        }

        @Override
        public void bindView(BaseRecyclerHolder holder, Conversation conversation, int position) {
            holder.setText(R.id.tv_recent_msg, conversation.getLastMessageContent());
            holder.setText(R.id.tv_recent_time, TimeUtil.getChatTime(false, conversation.getLastMessageTime()));
            //会话图标
            if (conversation.getAvatar() != null && !conversation.getAvatar().toString().equals("")) {
                ImageLoader.getInstance().displayImage(conversation.getAvatar().toString(), (CircleImageView) holder.getView(R.id.iv_recent_avatar),
                        ImageLoadOptions.getOptions());
            } else {
                holder.setImageResource(R.id.iv_recent_avatar, R.mipmap.head);
            }
//            holder.setImageView(conversation == null ? null : conversation.getAvatar().toString(), R.mipmap.head, R.id.iv_recent_avatar);

            //会话标题
            holder.setText(R.id.tv_recent_name, conversation.getcName());
            //查询指定未读消息数
            long unread = conversation.getUnReadCount();
            if (unread > 0) {
                holder.setVisible(R.id.tv_recent_unread, View.VISIBLE);
                holder.setText(R.id.tv_recent_unread, String.valueOf(unread));
                if (removeList.contains(position)) {
                    //标记为已读
                    BmobIMDBManager.getInstance(BmobIM.getInstance().getCurrentUid()).updateReceiveStatus(conversation.getcId());
                    //调用mianActivity中的检查红点
                    if (getActivity() instanceof Check) {
                        ((Check) getActivity()).logout();
                    }
                    holder.setVisible(R.id.tv_recent_unread, View.GONE);
                }
            } else {
                holder.setVisible(R.id.tv_recent_unread, View.GONE);
            }
            StickyViewHelper stickyViewHelper = new StickyViewHelper(context, holder.getView(R.id.tv_recent_unread), R.layout.includeview);

            setViewOut2InRangeUp(stickyViewHelper);
            setViewOutRangeUp(position, stickyViewHelper);
            setViewInRangeUp(stickyViewHelper);
            setViewInRangeMove(stickyViewHelper);
            setViewOutRangeMove(stickyViewHelper);
        }

        /**
         * view在范围外移动执行此Runnable
         *
         * @param stickyViewHelper
         */
        public void setViewOutRangeMove(StickyViewHelper stickyViewHelper) {
            stickyViewHelper.setViewOutRangeMoveRun(new Runnable() {
                @Override
                public void run() {
                    DisplayUtils.showToast(context, "ViewOutRangeMove");
                }
            });
        }

        /**
         * view在范围内移动指此此Runnable
         *
         * @param stickyViewHelper
         */
        private void setViewInRangeMove(StickyViewHelper stickyViewHelper) {
            stickyViewHelper.setViewInRangeMoveRun(new Runnable() {
                @Override
                public void run() {
//                    DisplayUtils.showToast(context, "ViewInRangeMove");
                }
            });
        }

        /**
         * view没有移出过范围，在范围内松手
         *
         * @param stickyViewHelper
         */
        private void setViewInRangeUp(StickyViewHelper stickyViewHelper) {
            stickyViewHelper.setViewInRangeUpRun(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();

                }
            });
        }

        /**
         * view移出范围，最后在范围外松手
         *
         * @param position
         * @param stickyViewHelper
         */
        private void setViewOutRangeUp(final int position, StickyViewHelper stickyViewHelper) {
            stickyViewHelper.setViewOutRangeUpRun(new Runnable() {
                @Override
                public void run() {
                    removeList.add(position);
                    notifyDataSetChanged();
                }
            });
        }

        /**
         * view移出过范围，最后在范围内松手执行次Runnable
         *
         * @param stickyViewHelper
         */
        private void setViewOut2InRangeUp(StickyViewHelper stickyViewHelper) {
            stickyViewHelper.setViewOut2InRangeUpRun(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

    }

    /**
     * fragment调用checkRedPoint
     */
    public interface Check {
        void logout();
    }
}