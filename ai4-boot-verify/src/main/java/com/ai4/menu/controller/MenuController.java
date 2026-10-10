package com.ai4.menu.controller;

import com.ai4.common.response.ApiResponse;
import com.ai4.menu.dto.CreateMenuRequest;
import com.ai4.menu.dto.MenuTreeResponse;
import com.ai4.menu.dto.UpdateMenuRequest;
import com.ai4.menu.model.Menu;
import com.ai4.menu.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口
 */
@RestController
@RequestMapping("/menus")
public class MenuController{

    private final MenuService menuService;

    public MenuController(MenuService menuService){
        this.menuService = menuService;
    }

    /**
     * 根据菜单ID查询菜单详情
     */
    @GetMapping("/{menuId}")
    public ApiResponse<Menu> get(@PathVariable Long menuId){
        return ApiResponse.success(menuService.get(menuId));
    }

    /**
     * 查询全部菜单
     */
    @GetMapping
    public ApiResponse<List<MenuTreeResponse>> listTree(){
        return ApiResponse.success(menuService.listTree());
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public ApiResponse<Menu> create(
            @Valid @RequestBody CreateMenuRequest request
    ) {
        return ApiResponse.success(menuService.create(request));
    }

    /**
     * 修改指定菜单
     */
    @PutMapping("/{menuId}")
    public ApiResponse<Menu> update(
            @PathVariable Long menuId,
            @Valid @RequestBody UpdateMenuRequest request
    ) {
        return ApiResponse.success(menuService.update(menuId, request));
    }

    /**
     * 删除没有子菜单的指定菜单
     */
    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ApiResponse.success(null);
    }
}
