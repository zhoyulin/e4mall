package cn.e4mall.cat.service.impl;

import cn.e4mall.cat.service.Cartservice;
import cn.e4mall.common.jedis.JedisClient;
import cn.e4mall.common.utils.E3Result;
import cn.e4mall.common.utils.JsonUtils;
import cn.e4mall.mapper.TbItemMapper;
import cn.e4mall.pojo.TbItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

public class CartServiceImpl implements Cartservice {

    @Autowired
    private JedisClient  jedisClient;

    @Value("${REDIS_CART__PRE}")
    private String  REDIS_CART__PRE;

    @Autowired
    private TbItemMapper  tbItemMapper;

    @Override
    public E3Result addCart(long userId, long itemId, int  num) {
        //向redis 添加购物车
        //数据类型是hash key:用户id  field: 商品id value:商品信息
        //判断商品是否存在
         Boolean  hexists= jedisClient.hexists(REDIS_CART__PRE +":"+ userId,itemId+"");
        // 如果存在数量相加
        if (hexists){
            String  json  = jedisClient.hget(REDIS_CART__PRE +":"+ userId,itemId+"");
              // 把json 转换成 Tbitem
            TbItem  item = JsonUtils.jsonToPojo(json,TbItem.class);
            item.setNum(item.getNum()+num);
            // 写回redis
            jedisClient.hset(REDIS_CART__PRE +":"+ userId,itemId+"", JsonUtils.objectToJson(item));
            return E3Result.ok();
        }
        // 如果不存在，根据商品id 取商品信息
        TbItem item= tbItemMapper.selectByPrimaryKey(itemId);
         // 设置购物车
        item.setNum(num);
        // 取一张图片
        String  image =  item.getImage();
        if(StringUtils.isNotBlank(image)){
            item.setImage(image.split(",")[0]);
        }
        // 添加到购物车列表
        jedisClient.hset(REDIS_CART__PRE +":"+ userId,itemId+"", JsonUtils.objectToJson(item));
        return E3Result.ok();
        //返回成功

    }


    public E3Result mergeCart(long userId, List<TbItem> itemList) {
        // 遍历列表
        //  把列表添加到购物车
        //  判断购物车是否有商品
        for (TbItem  tbItem: itemList){
             addCart(userId,tbItem.getId(),tbItem.getNum());
        }
        return E3Result.ok();
    }


    public List<TbItem> getCartList(long userId) {
        //        // 根据用户邪id 查询购物车列表
        List<String>  jsonList =  jedisClient.hvals(REDIS_CART__PRE +":"+ userId);
        List<TbItem>  itemList =  new ArrayList<>();
        for (String string: jsonList){
            // 创建一个Tbitem 对象
            TbItem  item = JsonUtils.jsonToPojo(string,TbItem.class);
            // 添加列表
            itemList.add(item);
        }
        return itemList;
    }


    public E3Result updateCartNum(long userId, long itemId, int num) {
        String  json =  jedisClient.hget(REDIS_CART__PRE +":"+ userId,itemId+"");
        //
        TbItem  tbItem = JsonUtils.jsonToPojo(json,TbItem.class);
        tbItem.setNum(num);
        jedisClient.hset(REDIS_CART__PRE +":"+ userId,itemId+"", JsonUtils.objectToJson(tbItem));

        return E3Result.ok();
    }


    public E3Result deleteCartItem(long userId, long itemId) {
        //删除购物车
        jedisClient.hdel(REDIS_CART__PRE +":"+ userId,itemId+"");
        return E3Result.ok();
    }


    public E3Result clearCartItem(long userId) {
        jedisClient.del(REDIS_CART__PRE +":"+ userId);
        return null;
    }
}
