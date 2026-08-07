package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.game.dto.RecipeShopVO;
import com.fruitisland.game.entity.GamePlayer;
import com.fruitisland.game.entity.PlayerRecipePurchase;
import com.fruitisland.game.entity.RecipeShopConfig;
import com.fruitisland.game.mapper.PlayerRecipePurchaseMapper;
import com.fruitisland.game.mapper.RecipeShopConfigMapper;
import com.fruitisland.game.service.GamePlayerService;
import com.fruitisland.game.service.PlayerRecipeService;
import com.fruitisland.game.service.RecipeShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeShopServiceImpl implements RecipeShopService {

    private final RecipeShopConfigMapper recipeShopConfigMapper;
    private final PlayerRecipePurchaseMapper playerRecipePurchaseMapper;
    private final GamePlayerService gamePlayerService;
    private final PlayerRecipeService playerRecipeService;

    @Override
    public RecipeShopVO listRecipes(Long playerId) {
        List<RecipeShopConfig> configs = recipeShopConfigMapper.selectList(
                new LambdaQueryWrapper<RecipeShopConfig>()
                        .eq(RecipeShopConfig::getEnabled, 1)
                        .orderByAsc(RecipeShopConfig::getSortOrder));

        Set<String> purchasedRecipeIds = playerRecipePurchaseMapper.selectList(
                        new LambdaQueryWrapper<PlayerRecipePurchase>()
                                .eq(PlayerRecipePurchase::getPlayerId, playerId))
                .stream()
                .map(PlayerRecipePurchase::getRecipeId)
                .collect(Collectors.toSet());

        List<RecipeShopVO.RecipeItem> items = configs.stream()
                .map(c -> new RecipeShopVO.RecipeItem(
                        c.getRecipeId(),
                        c.getRecipeName(),
                        c.getShopType(),
                        c.getPrice(),
                        c.getCategory(),
                        purchasedRecipeIds.contains(c.getRecipeId())))
                .collect(Collectors.toList());

        RecipeShopVO vo = new RecipeShopVO();
        vo.setRecipes(items);
        return vo;
    }

    @Override
    @Transactional
    public RecipeShopVO.RecipeItem buyRecipe(Long playerId, String recipeId) {
        // 1. 查找配方商店配置
        RecipeShopConfig config = recipeShopConfigMapper.selectOne(
                new LambdaQueryWrapper<RecipeShopConfig>()
                        .eq(RecipeShopConfig::getRecipeId, recipeId)
                        .eq(RecipeShopConfig::getEnabled, 1));
        if (config == null) {
            throw new RuntimeException("配方不存在或已停用: " + recipeId);
        }

        // 2. 检查是否已购买
        Long existingCount = playerRecipePurchaseMapper.selectCount(
                new LambdaQueryWrapper<PlayerRecipePurchase>()
                        .eq(PlayerRecipePurchase::getPlayerId, playerId)
                        .eq(PlayerRecipePurchase::getRecipeId, recipeId));
        if (existingCount > 0) {
            throw new RuntimeException("已购买过该配方");
        }

        // 3. 扣金币
        GamePlayer player = gamePlayerService.getById(playerId);
        if (player == null) throw new RuntimeException("玩家不存在");
        if (player.getGold() < config.getPrice()) {
            throw new RuntimeException("金币不足，需要 " + config.getPrice());
        }
        player.setGold(player.getGold() - config.getPrice());
        gamePlayerService.updateById(player);

        // 4. 记录购买
        PlayerRecipePurchase purchase = new PlayerRecipePurchase();
        purchase.setPlayerId(playerId);
        purchase.setRecipeId(recipeId);
        purchase.setPricePaid(config.getPrice());
        try {
            playerRecipePurchaseMapper.insert(purchase);
        } catch (DuplicateKeyException ignored) {
            throw new RuntimeException("已购买过该配方");
        }

        // 5. 授予配方永久使用权
        playerRecipeService.grantPermanent(playerId, recipeId, "RECIPE_SHOP");

        return new RecipeShopVO.RecipeItem(
                config.getRecipeId(),
                config.getRecipeName(),
                config.getShopType(),
                config.getPrice(),
                config.getCategory(),
                true);
    }
}
