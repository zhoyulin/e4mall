package cn.e4mall.cat.service;

import cn.e4mall.common.utils.E3Result;
import cn.e4mall.pojo.TbItem;

import java.util.List;

public interface Cartservice {

    E3Result  addCart(long  userId, long  itemId, int num);
    E3Result  mergeCart(long userId, List<TbItem> itemList);
    List<TbItem>  getCartList(long userId);
    E3Result   updateCartNum (long  userId, long  itemId, int num);
    E3Result   deleteCartItem(long  userId, long itemId);
    E3Result   clearCartItem(long  userId);
}
