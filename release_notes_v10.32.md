# PwnEyes v10.32 Release Notes

**Release Date:** July 3, 2025  
**Version Code:** 36  
**Version Name:** 10.32  

## 🔧 **CRITICAL UI FIX: Status Bar Overlap Resolved**

### **✅ Major Visual Layout Fix**
- **RESOLVED**: Fixed critical issue where "Home" text appeared underneath the Android status bar
- **IMPACT**: App content no longer overlaps with system UI elements (time, battery, network indicators)
- **COMPATIBILITY**: Enhanced Android 15 edge-to-edge display support
- **USER EXPERIENCE**: Significantly improved visual polish and professional appearance

## 🚀 **Key Improvements**

### **Edge-to-Edge Display Implementation**
- ✅ **Modern UI**: Implemented proper Android 15 edge-to-edge display handling
- ✅ **Window Insets**: Added comprehensive window insets management
- ✅ **Dynamic Padding**: Smart padding adjustment based on system UI dimensions
- ✅ **Status Bar**: Proper status bar transparency with content positioning

### **Layout Enhancements**
- 🎨 **AppBar Positioning**: Toolbar now correctly positioned below status bar
- 🎨 **Content Spacing**: Proper spacing maintained across all screen sizes
- 🎨 **Navigation Bar**: Enhanced bottom navigation bar handling
- 🎨 **Ad Container**: Improved positioning to avoid system UI overlap

### **Theme Modernization**
- 🌟 **Transparent Bars**: Status and navigation bars now transparent for modern look
- 🌟 **Display Cutouts**: Proper handling of notched and edge-to-edge displays
- 🌟 **System Integration**: Better integration with Android's system UI
- 🌟 **Visual Consistency**: Consistent appearance across all Android versions

## 🛠 **Technical Updates**

### **MainActivity Enhancements**
- Added `WindowCompat.setDecorFitsSystemWindows(window, false)` for edge-to-edge
- Implemented `setupWindowInsets()` method for proper inset handling
- Added `ViewCompat.setOnApplyWindowInsetsListener` for dynamic padding
- Enhanced system UI integration with proper lifecycle management

### **Layout Improvements**
- Updated `activity_main.xml` with `android:fitsSystemWindows="false"`
- Enhanced AppBarLayout system window inset handling
- Improved CoordinatorLayout behavior with system UI
- Optimized fragment container positioning

### **Theme Configuration**
- Configured transparent status bar: `@android:color/transparent`
- Configured transparent navigation bar: `@android:color/transparent`
- Added `android:windowLayoutInDisplayCutoutMode="shortEdges"`
- Enhanced status bar content visibility settings

## 📱 **User Experience**

### **Visual Improvements**
- ✅ **Professional Look**: App now has modern, polished appearance
- ✅ **Clear Text**: "Home" and all toolbar text properly visible
- ✅ **No Overlap**: System UI elements no longer interfere with app content
- ✅ **Consistent Layout**: Proper spacing maintained in all orientations

### **Compatibility**
- **Android Versions**: 7.0+ (API 24) - all supported versions
- **Screen Types**: Phone, tablet, foldable - all properly handled
- **Orientations**: Portrait and landscape - both work correctly
- **Display Features**: Standard, notched, edge-to-edge - all compatible

## 🔧 **For Developers**

### **Implementation Details**
```kotlin
// Edge-to-edge display setup
WindowCompat.setDecorFitsSystemWindows(window, false)

// Window insets handling
ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
    binding.appBarMain.setPadding(0, insets.top, 0, 0)
    windowInsets
}
```

### **Theme Configuration**
```xml
<item name="android:statusBarColor">@android:color/transparent</item>
<item name="android:navigationBarColor">@android:color/transparent</item>
<item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
```

## 📋 **Migration Notes**

### **From Previous Versions**
- **Automatic Update**: Users will receive this update automatically
- **Visual Change**: Users will notice improved layout immediately
- **No Data Loss**: All user data and settings preserved
- **No Action Required**: Update applies automatically

### **Build Information**
- **Build Status**: BUILD SUCCESSFUL in 27s
- **Compilation**: Zero errors, clean build
- **Testing**: Verified on multiple screen sizes and Android versions
- **Performance**: No impact on app performance or battery usage

## 🎯 **Why This Update Matters**

### **Critical UI Issue**
The status bar overlap was a significant visual problem that:
- Made the app appear unprofessional
- Interfered with user navigation
- Violated Android 15 display guidelines
- Created confusion for users

### **Resolution Benefits**
This fix provides:
- **Professional Appearance**: App now meets modern Android design standards
- **Better Usability**: Clear, unobstructed interface elements
- **Future Compatibility**: Ready for upcoming Android versions
- **Enhanced Brand**: Improved perceived quality and attention to detail

## 🔄 **Update Process**

### **For Users**
1. Update available through Google Play Store
2. Automatic update if enabled
3. Immediate visual improvement upon launch
4. No settings changes or data migration needed

### **For Developers**
1. New app bundle targets Android 15 (API 35)
2. Enhanced edge-to-edge display implementation
3. Modern window insets handling
4. Improved system UI integration

## 📞 **Support**

If you experience any issues with this update:
- **Email**: PwnEyes@proton.me
- **Subject**: "v10.32 Status Bar Fix"
- **Include**: Device model, Android version, screenshot if applicable

## 🎉 **Thank You**

Thank you for your patience while we resolved this critical UI issue. PwnEyes now provides a much more polished and professional user experience that meets modern Android design standards.

---

**Previous Release**: v10.31 (Android 15 compliance)  
**Next Release**: v10.33 (planned features and improvements)  
**Support**: PwnEyes@proton.me  
**GitHub**: https://github.com/dpogreba/pwneyes
