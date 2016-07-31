package com.xinguan14.jdyp.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.xinguan14.jdyp.MyVeiw.NineGridTestLayout;
import com.xinguan14.jdyp.R;
import com.xinguan14.jdyp.adapter.base.BaseListAdapter;
import com.xinguan14.jdyp.adapter.base.BaseListHolder;
import com.xinguan14.jdyp.bean.Post;
import com.xinguan14.jdyp.bean.User;
import com.xinguan14.jdyp.util.SysUtils;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.datatype.BmobRelation;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;

/**
 * Created by wm on 2016/7/18.
 * 用来显示动态的数据
 */
public class SquareListViewAdapter extends BaseListAdapter<Post> {

    //private FinalBitmap finalBitmap;
    public   BaseListHolder holder;
    private String postId;
    private int wh;

    //要传入的参数有当前的Activity，数据集。item的布局文件
    public SquareListViewAdapter(Context context, List<Post> list,int itemLayoutId){

        super(context,list,itemLayoutId);
        //根据屏幕的大小设置控件的大小
        this.wh=(SysUtils.getScreenWidth(context)- SysUtils.Dp2Px(context, 99))/3;

    }

    //给控件绑定数据
    @Override
    public void convert( final BaseListHolder holder, Post item) {

        this.holder =holder;
        this.postId = item.getObjectId();

        RelativeLayout rL = holder.getView(R.id.rl4);
        NineGridTestLayout gv_images = holder.getView(R.id.gv_images);

       // final Post gridViewItem = mDatas.get(position);
        String name = null,time = null,content = null,headpath = null,contentImageUrl = null;
        if(item !=null){
            name = item.getAuthor().getUsername();
            time = item.getCreatedAt();
            content = item.getContent();
            headpath = item.getAuthor().getAvatar();
            contentImageUrl = item.getImageurl();
        }
        //昵称
        if (name!=null&&!name.equals("")) {
            holder.setTextView(R.id.info_tv_name,name);
        }
        //是否含有图片，有图片则显示gridview
        if (contentImageUrl!=null&&!contentImageUrl.equals("")) {
            rL.setVisibility(View.VISIBLE);
            initInfoImages(gv_images,contentImageUrl);
        } else {
            rL.setVisibility(View.GONE);
        }
        //点赞的rem ,查询喜欢这个帖子的所有用户，因此查询的是用户表
        BmobQuery<User> query = new BmobQuery<User>();
        Post post = new Post();
        post.setObjectId(postId);
        //Log.i("info","查询的动态ID："+postId);
        //likes是Post表中的字段，用来存储所有喜欢该帖子的用户
        query.addWhereRelatedTo("likes", new BmobPointer(post));
        query.findObjects(mContext, new FindListener<User>() {
            @Override
            public void onSuccess(List<User> list) {
                String likesUser ="";
                for (int i= 0;i<list.size();i++){
                    likesUser +=list.get(i).getUsername()+",";

                }
                if (likesUser.length()!=0) {
                    holder.setTextView(R.id.tv_likes_names,likesUser);
                }
                Log.i("info","点赞用户："+likesUser);
            }

            @Override
            public void onError(int i, String s) {

            }
        });
        //发布时间
        if (time!=null&&!time.equals("")) {
            holder.setTextView(R.id.info_tv_time,time);
        }
        //内容
        if (content!=null&&!content.equals("")) {
            holder.setTextView(R.id.info_tv_content,content);
        }
        //头像
        if (headpath!=null&&!headpath.equals("")) {
            Glide
                    .with(mContext)
                    .load(headpath)
                    .placeholder(R.drawable.love)
                    .into((ImageView)holder.getView(R.id.info_iv_head));
        } else {
            holder.setImageResource(R.id.info_iv_head,R.drawable.love);
        }
        //点击头像的点击事件
        holder.getView(R.id.info_iv_head).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Toast.makeText(mContext, "点击了头像", Toast.LENGTH_LONG).show();
            }
        });
        //点赞
        holder.getView(R.id.iv_share_heart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User user = BmobUser.getCurrentUser(mContext, User.class);
                Post post = new Post();
                post.setObjectId(postId);
                //将当前用户添加到Post表中的likes字段值中，表明当前用户喜欢该帖子
                BmobRelation relation = new BmobRelation();
                //将当前用户添加到多对多关联中
                relation.add(user);
                //多对多关联指向`post`的`likes`字段
                post.setLikes(relation);
                post.update(mContext, new UpdateListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(mContext, "点赞成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int arg0, String arg1) {
                        // TODO Auto-generated method stub
                        Toast.makeText(mContext, "点赞失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    //初始化图片集，设定GridView的列数
    public void initInfoImages(NineGridTestLayout gv_images,final String imgUrl){
        if(imgUrl!=null&&!imgUrl.equals("")) {
            String[] imgs = imgUrl.split("#");//多张图片的URL一#分开

            List<String> list = new ArrayList<>();//图片url
            for (int i = 0; i < imgs.length; i++) {
                list.add(imgs[i]);
            }
            gv_images.setIsShowAll(false); //当传入的图片数超过9张时，是否全部显示
            gv_images.setSpacing(5); //动态设置图片之间的间隔
            gv_images.setUrlList(list); //最后再设置图片url


        }

    }

}
