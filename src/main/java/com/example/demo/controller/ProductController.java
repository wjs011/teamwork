package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.common.Result;
import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Resource
    private ProductMapper productMapper;

    /**
     * 获取商品列表
     */
    @GetMapping("/list")
    public Result<?> list() {
        try {
            LambdaQueryWrapper<Product> queryWrapper = Wrappers.<Product>lambdaQuery()
                    .orderByDesc(Product::getCreateTime);
            List<Product> products = productMapper.selectList(queryWrapper);
            return Result.success(products);
        } catch (Exception e) {
            return Result.error("500", "获取商品列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取商品
     */
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable String id) {
        try {
            Product product = productMapper.selectById(id);
            if (product == null) {
                return Result.error("404", "商品不存在");
            }
            return Result.success(product);
        } catch (Exception e) {
            return Result.error("500", "获取商品信息失败: " + e.getMessage());
        }
    }

    /**
     * 添加商品
     */
    @PostMapping
    public Result<?> add(@RequestBody Product product) {
        try {
            // 检查商品是否已存在
            Product existProduct = productMapper.selectById(product.getId());
            if (existProduct != null) {
                return Result.error("400", "商品已存在");
            }
            
            productMapper.insert(product);
            return Result.success();
        } catch (Exception e) {
            return Result.error("500", "添加商品失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品
     */
    @PutMapping
    public Result<?> update(@RequestBody Product product) {
        try {
            Product existProduct = productMapper.selectById(product.getId());
            if (existProduct == null) {
                return Result.error("404", "商品不存在");
            }
            
            productMapper.updateById(product);
            return Result.success();
        } catch (Exception e) {
            return Result.error("500", "更新商品失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable String id) {
        try {
            Product product = productMapper.selectById(id);
            if (product == null) {
                return Result.error("404", "商品不存在");
            }
            
            productMapper.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("500", "删除商品失败: " + e.getMessage());
        }
    }
} 