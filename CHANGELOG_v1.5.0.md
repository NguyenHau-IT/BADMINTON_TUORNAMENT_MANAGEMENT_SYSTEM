# 📋 CHANGELOG - BTMS v1.5.0

**Release Date**: January 11, 2026  
**From**: v1.2.2 → v1.5.0

---

## ✨ **Major Features Added**

### 1. 📊 **Advanced Analytics & Statistics Dashboard**
- Real-time tournament statistics tracking
- Player performance metrics and analytics
- Win rate calculations and ranking displays
- Visual charts and performance graphs
- Statistical export capabilities (Excel, PDF)

### 2. 📋 **Tournament Templates & Auto-Scheduling**
- Pre-built tournament format templates (Round-robin, Knockout, Swiss)
- Automatic schedule generation
- Bye management system
- Flexible seeding options

### 3. 🌍 **Internationalization (i18n) Support**
- Multi-language support framework
- Vietnamese (Tiếng Việt) - fully implemented
- English (English) - fully implemented
- Chinese (简体中文) - framework ready
- Dynamic language switching in UI
- Resource bundles for easy translation

### 4. 🏆 **Ranking & Seeding System**
- ELO-based ranking calculation
- Historical ranking tracking
- Smart seeding based on player history
- Seasonal leaderboards
- Lifetime rankings

### 5. 🔔 **Enhanced Notification System**
- Push notifications for score updates
- Email notifications for officials
- Match countdown reminders
- Player update notifications
- Notification preferences management

---

## 🔧 **Technical Improvements**

### Performance Optimizations
- Query optimization for better database performance
- Caching mechanism for frequently accessed data
- Improved threading with Java 21 virtual threads
- Reduced memory footprint

### Code Quality
- Better code organization and modularity
- Enhanced error handling and logging
- Improved test coverage
- Type-safe implementations

### Database Enhancements
- New tables for rankings and statistics
- Optimized indexes for common queries
- Better data integrity constraints
- Support for historical data tracking

---

## 🐛 **Bug Fixes**

- Fixed concurrent modification issues in multi-court scenarios
- Improved H2 TCP server stability
- Fixed UI refresh delays in scoreboard updates
- Corrected language encoding issues in Vietnamese UI
- Fixed network interface detection on IPv6-enabled systems

---

## 📈 **Breaking Changes**

⚠️ **Database Migration Required**:
```sql
-- New tables added in v1.5.0:
-- - BangXepHang (Rankings table)
-- - ThongKeThiDau (Statistics table)
-- - TemplateGiai (Tournament templates table)
-- - CauHinhNgonNgu (Language configuration table)

-- Run migration scripts:
-- database/v1.5.0_migration.sql
```

---

## 📚 **Documentation Updates**

- Updated [README.md](README.md) with v1.5.0 features
- Updated [README_VI.md](README_VI.md) with Vietnamese documentation
- Updated [README_EN.md](README_EN.md) with English documentation
- Added [API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) for REST endpoints
- Added [CONFIGURATION_GUIDE.md](docs/CONFIGURATION_GUIDE.md) for advanced setup

---

## 🔒 **Security Updates**

- Improved authentication security
- Enhanced SQL injection prevention
- Better input validation and sanitization
- Secure credential storage
- HTTPS-ready configuration

---

## 📦 **Dependency Updates**

| Package | Old Version | New Version | Reason |
|---------|------------|------------|--------|
| Spring Boot | 3.4.0 | 4.0.1 | Latest stable release |
| Jackson | 2.15.x | 2.17.x | Enhanced JSON processing |
| H2 Database | 2.1.x | 2.2.x | Bug fixes and performance |
| Thymeleaf | 3.1.x | 3.2.x | i18n improvements |

---

## 🚀 **Migration Guide from v1.2.2 to v1.5.0**

### Step 1: Backup
```bash
# Backup your current database
sqlserver backup BTMS to disk = 'backup_v1.2.2.bak'
```

### Step 2: Update Code
```bash
git fetch origin
git checkout v1.5.0
mvn clean install
```

### Step 3: Database Migration
```bash
# Run migration script
sqlcmd -i database/v1.5.0_migration.sql
```

### Step 4: Configuration
- Update `application.properties` with new settings
- Configure language preference in settings
- Adjust ranking calculation parameters if needed

### Step 5: Test
```bash
mvn test
# Verify all tests pass before deploying
```

---

## 📊 **Version Comparison**

| Feature | v1.2.2 | v1.5.0 |
|---------|--------|--------|
| Tournament Management | ✅ | ✅ Enhanced |
| Multi-Court Support | ✅ | ✅ Optimized |
| Real-time Updates | ✅ SSE + UDP | ✅ Improved |
| Statistics & Analytics | ❌ | ✅ New |
| Ranking System | ❌ | ✅ New |
| i18n Support | ❌ | ✅ New |
| Tournament Templates | ❌ | ✅ New |
| Auto-scheduling | ❌ | ✅ New |
| Advanced Notifications | ❌ | ✅ New |

---

## 🎯 **Roadmap for v1.6.0**

- 📱 Mobile native app (Flutter)
- 📹 Live streaming integration
- 🤖 AI-powered match prediction
- 💳 Payment gateway integration
- 👨‍⚖️ Judge management system
- ☁️ Cloud deployment support

---

## 📞 **Support & Feedback**

**Issues & Bug Reports**: [GitHub Issues](https://github.com/NguyenHau-IT/BTMS-OVR/issues)  
**Contact**: nguyenviethau.it.2004@gmail.com  
**License**: MIT License

---

**🎉 Thank you for using BTMS! Enjoy the new features!**
