# FOCUS PRO — Android Productivity, App Blocker & Digital Discipline System

**FOCUS PRO** विद्यार्थियों और उन लोगों के लिए बनाया गया एक प्रीमियम Android एप्लीकेशन है जो YouTube, Instagram, Telegram, Games और distracting वेबसाइटों पर वास्तविक नियंत्रण रखना चाहते हैं।

---

## बिना Android Studio के APK कैसे बनाएँ? (GitHub Automatic Build Guide - हिंदी में)

आपको अपने कंप्यूटर पर Android Studio या कोई Coding Tool इंस्टॉल करने की आवश्यकता **नहीं** है। बस नीचे दिए गए सरल चरणों का पालन करें:

### चरण 1: GitHub अकाउंट बनाएँ
1. अपने मोबाइल या कंप्यूटर के ब्राउज़र में **https://github.com** खोलें।
2. **Sign up** बटन पर क्लिक करें।
3. अपना Email, Password और Username डालकर अकाउंट बना लें और Email Verify करें।

### चरण 2: नया Repository बनाएँ
1. GitHub में लॉग-इन करने के बाद ऊपर दाईं ओर **`+`** आइकन पर क्लिक करें और **New repository** चुनें।
2. **Repository name** में `FOCUS-PRO` लिखें।
3. **Public** या **Private** चुनें (दोनों में GitHub Actions मुफ्त काम करता है)।
4. नीचे हरे रंग के **Create repository** बटन पर क्लिक करें।

### चरण 3: प्रोजेक्ट की फाइलें Upload करें
1. सबसे पहले अपने कंप्यूटर/मोबाइल पर `FOCUS_PRO_Android_Project.zip` को **Extract (Unzip)** करें ताकि अंदर की सभी फाइलें (`app`, `gradle`, `.github`, `build.gradle.kts`, `README.md`) बाहर आ जाएँ।
2. अपनी नई GitHub Repository पेज पर **uploading an existing file** लिंक पर क्लिक करें।
3. **ध्यान दें**: `.github` फोल्डर (जिसमें `workflows/android-build.yml` है) सहित **सभी फाइलें और फोल्डर** ड्रैग-एंड-ड्रॉप करें।
   *(यदि आपके कंप्यूटर पर `.github` छिपा हुआ है, तो File Explorer में "Show Hidden Files" चालू करें)*।
4. नीचे **Commit changes** बटन पर क्लिक करें।

### चरण 4: GitHub Actions चलाएँ और APK डाउनलोड करें
1. Upload होते ही ऊपर **Actions** टैब पर क्लिक करें।
2. आपको **"FOCUS PRO Android APK Build"** नाम का Workflow चलता हुआ दिखाई देगा (पीला आइकन)।
3. लगभग 3 से 5 मिनट प्रतीक्षा करें जब तक आइकन **हरा (Green Checkmark)** न हो जाए।
   - *(यदि कोई Error आए, तो उसी पेज पर नीचे `build-failure-logs` डाउनलोड करके देख सकते हैं)*।
4. हरे निशान पर क्लिक करें और नीचे **Artifacts** सेक्शन में जाएँ।
5. **`FOCUS_PRO-debug-apk`** पर क्लिक करके `.zip` डाउनलोड करें और उसे Unzip करें — अंदर **`app-debug.apk`** मिल जाएगी!

### चरण 5: मोबाइल में APK इंस्टॉल करें और Permissions दें
1. `app-debug.apk` को अपने Android फोन में भेजें और उस पर टैप करके **Install** करें (यदि "Unknown Sources" पूछे तो Allow करें)।
2. ऐप खोलें और Settings → **Permission Status** में जाकर ये 3 मुख्य अनुमतियाँ दें:
   - **Accessibility Service**: वास्तविक App Blocking के लिए।
   - **Usage Access**: वास्तविक Screen Time और App Usage Tracker के लिए।
   - **VPN Permission** (ऐप के Website Blocker सेक्शन में): स्थानीय DNS आधारित वेबसाइट ब्लॉकिंग के लिए (HTTPS ट्रैफिक डिक्रिप्ट नहीं होता)।
