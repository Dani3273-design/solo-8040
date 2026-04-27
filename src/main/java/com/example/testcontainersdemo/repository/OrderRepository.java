package com.example.testcontainersdemo.repository;

import com.example.testcontainersdemo.entity.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private final RowMapper<Order> rowMapper = (rs, rowNum) -> {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setOrderNo(rs.getString("order_no"));
        order.setProductName(rs.getString("product_name"));
        order.setQuantity(rs.getInt("quantity"));
        order.setPrice(rs.getBigDecimal("price"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        order.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return order;
    };
    
    public List<Order> findAll() {
        String sql = "SELECT * FROM orders";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public Optional<Order> findById(Long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        List<Order> orders = jdbcTemplate.query(sql, rowMapper, id);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.get(0));
    }
    
    public Optional<Order> findByOrderNo(String orderNo) {
        String sql = "SELECT * FROM orders WHERE order_no = ?";
        List<Order> orders = jdbcTemplate.query(sql, rowMapper, orderNo);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.get(0));
    }
    
    public Order save(Order order) {
        if (order.getId() == null) {
            return insert(order);
        } else {
            return update(order);
        }
    }
    
    private Order insert(Order order) {
        String sql = "INSERT INTO orders (order_no, product_name, quantity, price, status) VALUES (?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, order.getOrderNo());
            ps.setString(2, order.getProductName());
            ps.setInt(3, order.getQuantity());
            ps.setBigDecimal(4, order.getPrice());
            ps.setString(5, order.getStatus() != null ? order.getStatus() : "PENDING");
            return ps;
        }, keyHolder);
        
        Long generatedId = keyHolder.getKey().longValue();
        return findById(generatedId).orElse(null);
    }
    
    private Order update(Order order) {
        String sql = "UPDATE orders SET product_name = ?, quantity = ?, price = ?, status = ? WHERE id = ?";
        
        jdbcTemplate.update(sql,
                order.getProductName(),
                order.getQuantity(),
                order.getPrice(),
                order.getStatus(),
                order.getId());
        
        return findById(order.getId()).orElse(null);
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM orders WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM orders WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
