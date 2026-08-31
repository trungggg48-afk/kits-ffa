# Hướng Dẫn Sử Dụng & Quản Trị Hệ Thống HyperFFA (Bản Mới Nhất)

Tài liệu hướng dẫn chi tiết cách người chơi và quản trị viên sử dụng hệ thống **HyperFFA** (Kit, Scoreboard, Killstreak, Leaderboard, PlaceholderAPI & Combat) trên máy chủ **Paper & Folia 1.21.x**.

---

## 1. Dành Cho Người Chơi (Player Guide)

### A. Mở Giao Diện KIT ROOM
- Gõ lệnh:
  ```text
  /kit
  ```
  hoặc gõ tắt để nạp trực tiếp kit:
  ```text
  /kit1
  /kit2
  /kit3
  /kit4
  /kit5
  /kit6
  ```

### B. Thao Tác Trong Menu Kit Room
- **Chuột Trái (Left Click)**: Nạp bộ trang bị của slot đó vào người bạn.
- **Chuột Phải (Right Click)**: Chuyển sang chế độ **Chỉnh Sửa Kit** (Edit Mode).
- **Slot VIP/Premium (Khóa)**: Hiển thị bằng Thuốc nhuộm xám (`GRAY_DYE`) kèm thông báo *You don't have access to this kit!*.
- **Các nút công cụ (Hàng 5)**:
  - `TNT` (Slot 39): Xóa sạch đồ đang chỉnh sửa (Clear Inventory).
  - `BARREL` (Slot 40): Mở kho vật phẩm (Item Room).
  - `WRITABLE_BOOK` (Slot 41): Nạp ngay lập tức bộ trang bị mẫu của Server (Premade Kit).

### C. Sử Dụng Giao Diện ITEM ROOM
- **Cơ chế lấy đồ & Refill**: Khi bấm vào một món đồ bất kỳ, món đồ đó sẽ được chuyển vào túi của bạn và ô đó trên GUI sẽ bị lấy ra. Khi bấm vào nút `Refill`, toàn bộ 45 ô trên GUI sẽ được lấp đầy lại 100%.
- **Thanh chuyển danh mục nằm ở hàng 6 (Slots 45 - 53)**:
  - `Slot 45` (`AMETHYST_SHARD`): **Refill** (Làm đầy lại kho đồ Item Room).
  - `Slot 47` (`NETHERITE_SWORD`): **Gear** (Mace 1 [Breach IV], Mace 2 [Wind Burst III / Density V], Giáp & Vũ khí Netherite/Diamond Max Enchant).
  - `Slot 48` (`POTION`): **Potions** (7 cột thuốc ném + 2 cột mũi tên hiệu ứng).
  - `Slot 49` (`TOTEM_OF_UNDYING`): **Consumables** (18 Totem of Undying, Táo vàng thường, Ngọc Ender, EXP...).
  - `Slot 50` (`END_CRYSTAL`): **Explosives** (9 Stack khoáng sản Đồng, Sắt, Kim cương, Vàng, Lục bảo, Lưu ly, Netherite, Đá đỏ, Than đá; Obsidian, Powered Rails, Glowstone, **Xe mỏ TNT**).
  - `Slot 51` (`PURPLE_SHULKER_BOX`): **Miscellaneous** (Xô nước, Tuyết bột, Lava, Mảnh rèn giáp Trim [Silence, Vex, Ward], Mảnh nâng cấp Netherite, Khối mật ong, Slime, Bê tông 9 màu...).
  - `Slot 53` (`BARRIER`): **Quay Lại / Lưu Kit**.

### D. Các Lệnh Thống Kê & Bảng Xếp Hạng
- `/killstreak`: Xem chuỗi kill hiện tại và chuỗi tốt nhất.
- `/stats [player]`: Xem hồ sơ K/D, chuỗi kill, thời gian chơi, xu và tiền.
- `/topkills`: Xem Top 10 người chơi hạ gục nhiều nhất + thứ hạng cá nhân (`#-1`).
- `/topdeaths`: Xem Top 10 người chơi chết nhiều nhất.
- `/toptime`: Xem Top 10 người chơi online lâu nhất (dạng `140h`, `99h`).
- `/discord`: Nhận link tham gia máy chủ Discord.
- `/rtpq`: Tham gia hàng chờ trận đấu FFA.

---

## 2. Hỗ Trợ PlaceholderAPI (PAPI)

Hệ thống cung cấp placeholder cho Killstreak hiển thị trên Scoreboard, Tab, Chat hoặc Hologram:

| Placeholder | Ý Nghĩa | Kết Quả Mẫu |
| :--- | :--- | :--- |
| `%hyperffa_killstreak%` / `%hyperffa_streak%` | Chuỗi kill hiện tại của người chơi đó | `5` |
| `%hyperffa_best_killstreak%` / `%hyperffa_best_streak%` | Kỷ lục chuỗi kill cao nhất của người giỏi nhất toàn server | `18` |
| `%hyperffa_best_killstreak_player%` | Tên của người giữ kỷ lục killstreak cao nhất server | `Maz52` |

---

## 3. Dành Cho Quản Trị Viên (Admin Guide)

### Thiết Lập Nhanh Trực Tiếp Bằng Túi Đồ Của Admin
1. **Tạo Mode Mới**: `/kitadmin mode create <tên_mode>`
2. **Lưu Kit Mẫu (Premade Kit)**:
   - Sắp xếp túi đồ đầy đủ -> Gõ: `/kitadmin setpremade <mode> Default`
3. **Lưu Danh Mục Đồ Item Room**:
   - Gõ: `/kitadmin setcategory <mode> <gear|potions|consumables|explosions|miscellaneous>`
4. **Nạp Kit Cho Người Chơi Khác**:
   - `/kitadmin give <player> <mode> <slot>`
5. **Tải Lại Cấu Hình**: `/kitadmin reload`

---

## 4. Tương Thích Folia & Paper 1.21
- Plugin hoàn toàn hỗ trợ nền tảng đa luồng **Folia 1.21** (Regionized Server) và **Paper/Purpur 1.21**.
- Tự động điều phối các tác vụ bất đồng bộ và khu vực an toàn qua `PlatformScheduler`.
