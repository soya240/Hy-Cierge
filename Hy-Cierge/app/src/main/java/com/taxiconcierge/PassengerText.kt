package com.taxiconcierge

/** 승객 화면(Figma P01~P25) 문구. 순서: 한국어, English, 中文, 日本語 */
object PT {
    val sample = T("예시", "Sample", "示例", "サンプル")

    // P03 어디로
    val tapToSpeak = T("눌러서 말하기", "Tap to speak", "点击说话", "タップして話す")
    val listening = T("듣고 있어요…", "Listening…", "正在聆听…", "聞いています…")
    val typeInstead = T("직접 고르기", "Type instead", "手动选择", "文字で選ぶ")
    val chooseLanguage = T("언어 선택", "Choose language", "选择语言", "言語を選ぶ")

    // 목적지 고르기
    val chooseDest = T("목적지를 골라 주세요", "Choose your destination", "请选择目的地", "行き先を選んでください")
    val chooseDestSub = T("장소와 내릴 곳을 함께 골라 주세요.", "Pick a place and where you'd like to get off.", "请选择地点和下车位置。", "場所と降りる場所を選んでください。")

    // P04 확인
    val checkTitle = T("목적지를 확인해 주세요", "Check your destination", "请确认目的地", "行き先を確認してください")
    val checkSub = T("기사님께 보내기 전에 요청을 확인해 주세요.", "Review the request before sending it to your driver.", "发送给司机前请确认请求内容。", "ドライバーに送る前にリクエストを確認してください。")
    val yourRequest = T("내 요청", "Your request", "您的请求", "リクエスト")
    val change = T("변경", "Change", "更改", "変更")
    val sendToDriver = T("기사님께 보내기", "Send to driver", "发送给司机", "ドライバーに送る")
    val editText = T("다시 고르기", "Edit text", "修改", "編集")
    val recordAgain = T("다시 말하기", "Record again", "重新录音", "もう一度話す")

    // P05 대기
    val waiting = T("기사님 확인을 기다리는 중", "Waiting for your driver", "等待司机确认", "ドライバーの確認待ち")
    val requestSent = T("요청 보냄", "Request sent", "请求已发送", "送信済み")
    val editRequest = T("요청 수정", "Edit request", "修改请求", "リクエストを修正")
    val viewSent = T("보낸 요청 보기", "View sent request", "查看已发送的请求", "送った内容を見る")
    val sentInKorean = T("기사님께는 한국어로 전달됐어요", "Your driver sees it in Korean", "司机看到的是韩语", "ドライバーには韓国語で届きました")

    // 목적지 확정
    val destConfirmed = T("목적지 확정", "Destination confirmed", "目的地已确认", "行き先が確定しました")
    val exploreOnTheWay = T("가는 길 둘러보기", "Explore on the way", "沿途探索", "道中を楽しむ")

    // P06 홈
    val digitalKit = T("디지털 키트", "Digital kit", "数字工具包", "デジタルキット")
    val digitalKitSub = T("지도, 택시 등 여행에 쓸 앱을 찾아보세요.", "Find the apps you'll use for maps, taxis and more.", "找到地图、打车等旅行必备应用。", "地図やタクシーなど、旅行で使うアプリを探せます。")
    val near = T("목적지 주변", "Near your destination", "目的地周边", "目的地の周辺")
    val nearSub = T("내리는 곳 근처의 장소와 이야기를 알아보세요.", "Discover local places and stories around your stop.", "发现下车地点附近的景点和故事。", "降りる場所の近くのスポットや物語を紹介します。")
    val travelTools = T("여행 도구", "Travel tools", "旅行工具", "旅行ツール")
    val travelToolsSub = T("식당 찾기, 하차 지점 변경, 환율 계산을 할 수 있어요.", "Find a restaurant, change your drop-off or convert prices.", "找餐厅、更改下车地点或换算价格。", "レストラン探し、降車地点の変更、価格の換算ができます。")
    val translate = T("통역", "Translate", "翻译", "通訳")

    // P07 디지털 키트
    val kitSub = T("휴대폰으로 코드를 스캔하면 앱 스토어가 열려요.", "Scan a code with your phone to open the app store.", "用手机扫码即可打开应用商店。", "スマホでコードを読み取るとアプリストアが開きます。")
    val previous = T("이전", "Previous", "上一个", "前へ")
    val next = T("다음", "Next", "下一个", "次へ")

    // P08 주변
    val nearTitleSub = T("내리는 곳 근처의 장소와 이야기예요.", "Places and stories around your stop.", "下车地点附近的景点和故事。", "降りる場所の近くのスポットと物語です。")
    val localStory = T("로컬 이야기", "Local story", "本地故事", "ローカルストーリー")
    val photoSoon = T("사진", "Photo", "照片", "写真")

    // P09 장소 상세
    val yourDropOff = T("내리는 곳", "Your drop-off", "下车地点", "降車地点")
    val sendToPhone = T("휴대폰으로 보내기", "Send to phone", "发送到手机", "スマホに送る")
    val visitingInfo = T("방문 정보 듣기", "Visiting information", "收听参观信息", "訪問情報を聞く")

    // P10 여행 도구
    val toolsSub = T("식사 계획, 하차 지점 확인, 가격 환산을 해 보세요.", "Plan a meal, check your drop-off or convert a price.", "规划用餐、确认下车地点或换算价格。", "食事の計画、降車地点の確認、価格の換算ができます。")
    val findFood = T("식당 찾기", "Find food", "找美食", "食事を探す")
    val findFoodSub = T("시장 노점을 둘러보거나 식당을 예약해요.", "Browse market stalls or book a restaurant.", "逛市场小摊或预订餐厅。", "市場の屋台を見たり、レストランを予約できます。")
    val changeDrop = T("하차 지점 변경", "Change drop-off", "更改下车地点", "降車地点の変更")
    val changeDropSub = T("지금 내릴 곳과 다른 입구를 비교해요.", "Compare the current stop with another entrance.", "比较当前下车点和其他入口。", "今の降車地点と別の入口を比べられます。")
    val convertCurrency = T("환율 계산", "Convert currency", "汇率换算", "通貨換算")
    val convertCurrencySub = T("원화 가격을 %s(으)로 비교해요.", "Compare a price in Korean won and %s.", "将韩元价格换算成%s。", "韓国ウォンの価格を%sと比べられます。")

    // P11 식당
    val findFoodNearby = T("근처 식당 찾기", "Find food nearby", "查找附近美食", "近くで食事を探す")
    val findFoodNearbySub = T("%s 근처에서 어떻게 드실지 골라 보세요.", "Choose how you'd like to eat near %s.", "选择在%s附近的用餐方式。", "%sの近くでの食事を選んでください。")
    val streetFood = T("길거리 음식", "Street food nearby", "附近街头小吃", "近くの屋台グルメ")
    val streetFoodSub = T("바로 들어갈 수 있는 노점에서 먹어볼 메뉴예요.", "See what to try at the walk-in stalls.", "看看可以直接去的小摊有什么好吃的。", "すぐ入れる屋台で食べたいメニューです。")
    val bookTable = T("식당 예약", "Book a table", "预订餐厅", "テーブルを予約")
    val bookTableSub = T("휴대폰으로 식당과 예약 가능한 시간을 찾아보세요.", "Look up restaurants and reservation times on your phone.", "用手机查找餐厅和可预约时间。", "スマホでレストランと予約できる時間を探せます。")
    val seeBooking = T("예약 앱 받기", "See booking options", "查看预订方式", "予約方法を見る")

    // P12 하차 지점 변경
    val confirmRequest = T("요청을 확인해 주세요", "Confirm your request", "请确认您的请求", "リクエストを確認してください")
    val confirmRequestSub = T("기사님이 확인한 뒤 경로가 바뀌어요.", "Your driver will review the change before the route is updated.", "司机确认后路线才会更新。", "ドライバーが確認してからルートが変わります。")
    val currentDrop = T("지금 내릴 곳", "Current drop-off", "当前下车点", "現在の降車地点")
    val selected = T("선택함", "Selected", "已选择", "選択中")
    val changeSelection = T("다른 곳 고르기", "Change selection", "重新选择", "選び直す")
    val sendChange = T("변경 요청 보내기", "Send change request", "发送更改请求", "変更をリクエスト")
    val changeSent = T("기사님께 변경 요청을 보냈어요", "Change request sent to your driver", "已向司机发送更改请求", "ドライバーに変更をリクエストしました")

    // P13 환율
    val convertTitle = T("가격 환산", "Convert a price", "价格换算", "価格を換算")
    val convertSub = T("원화 금액을 고르면 예상 금액을 보여 드려요.", "Choose an amount in Korean won to see its estimated value.", "选择韩元金额即可查看估算金额。", "ウォンの金額を選ぶと、目安の金額を表示します。")
    val amountKrw = T("원화 금액", "Amount in KRW", "韩元金额", "ウォンの金額")
    val estimatedIn = T("%s 예상 금액", "Estimated amount in %s", "估算金额（%s）", "%sでの目安")
    val sampleRate = T("예시 환율", "Sample rate", "示例汇率", "サンプルレート")
    val disclaimer = T("이해를 돕기 위한 예시예요. 실제 환율과 수수료는 다를 수 있어요.", "For illustration only. Rates and fees can vary.", "仅供参考，实际汇率和手续费可能不同。", "参考用の例です。実際のレートや手数料は異なる場合があります。")
    val currencyName = T("원", "US dollars", "人民币", "日本円")

    // P15 도착
    val almost = T("곧 도착해요", "You're almost there", "即将到达", "まもなく到着です")
    val almostSub = T("내리기 전에 두고 가는 물건이 없는지 확인해 주세요.", "Please check for any belongings before you leave.", "下车前请检查随身物品。", "降りる前にお忘れ物がないかご確認ください。")
    val keepLinks = T("장소와 여행 링크를 휴대폰에 저장해 두세요.", "Keep your place and travel links on your phone.", "把地点和旅行链接保存到手机上。", "場所と旅行リンクをスマホに保存しておきましょう。")

    // 시트
    val scanWithPhone = T("휴대폰 카메라로 스캔하세요", "Scan with your phone camera", "请用手机相机扫描", "スマホのカメラで読み取ってください")
    val openInMaps = T("지도에서 열기", "Open in Maps", "在地图中打开", "地図で開く")
    val close = T("닫기", "Close", "关闭", "閉じる")
    val translateSub = T("문장을 누르면 기사님께 한국어로 들려요.", "Tap a phrase. Your driver hears it in Korean.", "点击句子，司机会听到韩语。", "フレーズをタップすると、ドライバーに韓国語で伝わります。")
    val driverHears = T("기사님이 듣는 말", "Your driver hears", "司机听到的", "ドライバーに伝わる言葉")

    // 음성 우선 흐름 (P03A·P03C·P03·P03D·P03B·P27)
    val speakNaturally = T("편하게 말씀하세요", "Speak in your language", "请用您的语言说", "あなたの言葉で話してください")
    val listeningBadge = T("듣는 중", "Listening", "正在聆听", "聞いています")
    val yourRequestCap = T("내 요청", "Your Request", "您的请求", "リクエスト")
    val changeLanguage = T("언어 바꾸기", "CHANGE LANGUAGE", "更改语言", "言語を変更")
    val unclearTitle = T("언어를 알아듣지 못했어요", "We couldn't detect your language", "未能识别您的语言", "言語を認識できませんでした")
    val unclearSub = T("문장으로 다시 말하거나 언어를 골라 주세요.", "Try saying a full sentence or choose your language.", "请说一整句话，或选择您的语言。", "文章で話すか、言語を選んでください。")
    val whatWeHeard = T("들린 말", "What we heard", "我们听到的", "聞き取った内容")
    val nothingHeard = T("(아무 말도 들리지 않았어요)", "(We didn't hear anything)", "（没有听到声音）", "（何も聞こえませんでした）")
    val tryAgainSpeak = T("다시 말하기", "Try speaking again", "再说一次", "もう一度話す")
    val reviewRequest = T("요청 확인", "Review request", "确认请求", "リクエストを確認")
    val typeTitle = T("목적지를 입력하세요", "Type your destination", "输入目的地", "行き先を入力")
    val typeSub = T("장소 이름, 주소, 입구를 입력해 주세요.", "Enter a place name, street address or entrance.", "请输入地点名称、地址或入口。", "場所の名前、住所、入口を入力してください。")
    val destinationCap = T("목적지", "DESTINATION", "目的地", "行き先")
    val placeholder = T("장소 또는 주소", "Place or address", "地点或地址", "場所または住所")
    val orPick = T("또는 바로 고르기", "Or pick a place", "或直接选择", "または選ぶ")
    val editTitle = T("목적지 수정", "Edit your destination", "修改目的地", "行き先を修正")
    val editSub = T("보내기 전에 주소나 입구를 확인해 주세요.", "Check the address or describe the entrance before sending.", "发送前请确认地址或入口。", "送る前に住所や入口を確認してください。")
    val requestIn = T("%s 요청", "REQUEST IN %s", "%s请求", "%sのリクエスト")
    val keyboardNote = T("입력칸을 누르면 키보드가 나와요.", "The keyboard appears when you select the field.", "点击输入框会弹出键盘。", "入力欄を選ぶとキーボードが表示されます。")
    val saveChanges = T("저장", "Save changes", "保存修改", "変更を保存")

    // 디지털 키트 진행
    val kitGotIt = T("받았어요", "Got it", "已获取", "入れました")
    val kitAdded = T("추가됨", "added", "已添加", "追加済み")
    val nextNear = T("다음: 목적지 주변", "Next: Near your destination", "下一步：目的地周边", "次へ：目的地の周辺")

    val nextTools = T("다음: 여행 도구", "Next: Travel tools", "下一步：旅行工具", "次へ：旅行ツール")
    val backOverview = T("처음 화면으로", "Back to overview", "返回概览", "概要に戻る")

    // Figma P03C·P05·P06
    val finish = T("완료", "Finish", "完成", "完了")
    val comfort = T("기사님이 편안하게 모셔다 드릴게요", "Your driver will get you there comfortably", "司机会舒适地送您到达", "ドライバーが快適にお連れします")
    val translateCaps = T("통역", "TRANSLATE", "翻译", "通訳")
    val cardKit = T("한국 필수 앱 받기", "Download Essential Apps for Korea", "下载韩国必备应用", "韓国の必須アプリを入手")
    val cardNear = T("로컬\n큐레이션", "Local\nCuration", "本地\n精选", "ローカル\nキュレーション")
    val cardTools = T("여행\n도우미", "Travel\nAssistance", "旅行\n助手", "旅行\nアシスタンス")
    val kitAllDone = T("앱 4개를 모두 받았어요", "All 4 apps added", "4个应用已全部添加", "4つのアプリをすべて追加しました")

    // 언어 선택 휠
    val changeLanguageCaps = T("언어 변경", "CHANGE LANGUAGE", "更改语言", "言語を変更")
    val done = T("완료", "Done", "完成", "完了")
    // P08·P09·P09A·P14·P22
    val nearDest = T("%s 근처", "Near your Destination '%s'", "%s附近", "%s周辺")
    val nearSpotsSub = T("시장, 물길 산책로, 왕실 사당이 내리는 곳 근처에 있어요.", "A market, a streamside walk and a royal shrine near your stop.", "下车点附近有市场、溪边步道和王室祠堂。", "降りる場所の近くに市場、川沿いの散歩道、王室の祠堂があります。")
    val visiting = T("%s 방문 정보", "Visiting %s", "参观%s", "%sを訪ねる")
    val visitingSub = T("내리기 전에 주소와 이용 정보를 챙기세요.", "Keep the address and practical details handy before you step out.", "下车前请记好地址和实用信息。", "降りる前に住所と実用情報を確認しましょう。")
    val address = T("주소", "Address", "地址", "住所")
    val hoursPay = T("운영 시간·결제", "Hours and payment", "营业时间与付款", "営業時間・支払い")
    val checkStall = T("가게마다 확인", "Check at each stall", "请在各摊位确认", "各店舗で確認")
    val hoursDiffer = T("시장과 먹자골목 운영 시간이 다를 수 있어요.", "Market and food-street hours can differ.", "市场和美食街的营业时间可能不同。", "市場と屋台通りの営業時間は異なる場合があります。")
    val takePlans = T("일정을 휴대폰에 담아 가세요", "Take your plans with you", "把行程带在手机上", "予定をスマホに持っていきましょう")
    val takePlansSub = T("스캔하면 %s 안내를 휴대폰에서 볼 수 있어요.", "Scan to keep the %s guide on your phone.", "扫描即可在手机上查看%s指南。", "スキャンすると%sのガイドをスマホで見られます。")
    val officialGuide = T("VISITKOREA 공식 안내 · 주소, 운영 시간, 길 안내", "Official VISITKOREA guide · address, opening hours and directions.", "VISITKOREA 官方指南 · 地址、营业时间和路线", "VISITKOREA 公式ガイド · 住所、営業時間、行き方")
    val scanGuide = T("스캔해서 안내 보기", "Scan for the guide", "扫描查看指南", "スキャンしてガイドを見る")
    val sendToPhoneTitle = T("휴대폰으로 보내기", "Send to phone", "发送到手机", "スマホに送る")
    val sendSub = T("스캔하면 이 장소의 공식 안내가 열려요.", "Scan to open the official guide for this place.", "扫描即可打开该地点的官方指南。", "スキャンするとこの場所の公式ガイドが開きます。")
    val goodToKnow = T("알아두면 좋아요", "Good to know", "温馨提示", "知っておくと便利")
    val goodToKnowTitle = T("가고 싶은 입구를 정하세요", "Choose an access point", "选择入口", "入口を選びましょう")
    val goodToKnowSub = T("가려는 구간의 입구와 길을 미리 확인하세요.", "Check access and directions for the part you want to visit.", "请提前确认想去区域的入口和路线。", "行きたい場所の入口と道順を確認しましょう。")
    // P16 통역
    val talkTitle = T("기사님과 대화하기", "Talk to your driver", "和司机对话", "ドライバーと話す")
    val talkSub = T("목소리로 기사님과 이야기해 보세요.", "Use your voice to communicate with your driver.", "用语音和司机交流。", "声でドライバーと話しましょう。")
    val you = T("나", "You", "我", "あなた")
    val driverTranslated = T("기사님 · 한국어에서 번역", "Driver · translated from Korean", "司机 · 译自韩语", "ドライバー · 韓国語から翻訳")
    val speakToDriver = T("내 언어로 기사님께 말하세요.", "Speak to your driver in your language.", "用您的语言和司机说话。", "あなたの言葉でドライバーに話してください。")
    val startListening = T("듣기 시작", "Start listening", "开始聆听", "聞き取り開始")
    val listeningDots = T("듣는 중…", "Listening…", "正在聆听…", "聞いています…")
    val speakInYourLang = T("내 언어로 말하세요.", "Speak in your language.", "请用您的语言说。", "あなたの言葉で話してください。")
    val finishListening = T("듣기 끝내기", "Finish listening", "结束聆听", "聞き取りを終える")
    val listeningCaps = T("듣는 중…", "LISTENING…", "正在聆听…", "聞いています…")
    val talkEmpty = T("오른쪽 카드를 눌러 말해 보세요.", "Tap the card on the right and speak.", "点击右侧卡片开始说话。", "右のカードをタップして話してください。")
    // P12A·P24·P24A 하차
    val chooseDrop = T("내릴 곳을 고르세요", "Choose a drop-off point", "选择下车地点", "降車地点を選んでください")
    val chooseDropSub = T("변경이 확정될 때까지 지금 내릴 곳은 그대로예요.", "Your current stop stays in place until a change is confirmed.", "在确认更改前，当前下车点保持不变。", "変更が確定するまで今の降車地点はそのままです。")
    val alternative = T("다른 입구", "Alternative", "其他入口", "別の入口")
    val selectX = T("%s 선택", "Select %s", "选择%s", "%sを選ぶ")
    val waitingDriver = T("기사님 확인을 기다리는 중", "Waiting for your driver", "等待司机确认", "ドライバーの確認待ち")
    val stillCurrent = T("지금 내릴 곳은 아직 %s예요.", "Your current drop-off is still the %s.", "当前下车点仍是%s。", "現在の降車地点はまだ%sです。")
    val pending = T("대기 중", "Pending", "待确认", "確認中")
    val changeRequested = T("변경 요청 보냄", "Change request sent", "已发送更改请求", "変更をリクエストしました")
    val backTools = T("여행 도구로 돌아가기", "Back to travel tools", "返回旅行工具", "旅行ツールに戻る")
    val dropUpdated = T("내릴 곳이 바뀌었어요", "Drop-off updated", "下车地点已更新", "降車地点が変わりました")
    val driverConfirmed = T("기사님이 변경을 확인했어요.", "Your driver has confirmed the change.", "司机已确认更改。", "ドライバーが変更を確認しました。")
    val confirmed = T("확정", "Confirmed", "已确认", "確定")
    val useXFor = T("%s(으)로 가면 돼요.", "Use %s.", "请从%s进入。", "%sを使ってください。")
    // P26·P11A·P11B 식당
    val foodPrefs = T("음식 취향", "Food preferences", "饮食偏好", "食の好み")
    val foodPrefsSub = T("1인 기준 음식 종류와 예산을 고르세요.", "Pick a cuisine and budget per person.", "请选择菜系和人均预算。", "料理の種類と1人あたりの予算を選んでください。")
    val cuisine = T("음식 종류", "Cuisine", "菜系", "料理")
    val budget = T("예산", "Budget", "预算", "予算")
    val anyCuisine = T("모든 음식", "Any cuisine", "不限菜系", "すべての料理")
    val koreanFood = T("한식", "Korean food", "韩餐", "韓国料理")
    val anyBudget = T("예산 상관없음", "Any budget", "不限预算", "予算指定なし")
    val under30k = T("₩30,000 이하", "Under ₩30,000", "₩30,000以下", "₩30,000以下")
    val showRestaurants = T("식당 보기", "Show restaurants", "查看餐厅", "レストランを見る")
    val streetTitle = T("시장 길거리 음식", "Market street food", "市场街头小吃", "市場の屋台グルメ")
    val streetDesc = T("철판에 부쳐 내는 녹두 빈대떡을 찾아보세요.", "Look for bindaetteok, a mung bean pancake cooked on the griddle.", "找找在铁板上煎的绿豆煎饼。", "鉄板で焼く緑豆のチヂミ、ピンデトックを探してみてください。")
    val beforeOrder = T("주문하기 전에", "Before you order", "点餐前", "注文する前に")
    val walkIn = T("바로 들어가는 노점", "Walk-in stalls", "无需预约的小摊", "予約不要の屋台")
    val confirmPrice = T("가격과 결제 방법을 노점에서 확인하세요.", "Confirm the price and payment method at the stall.", "请在摊位确认价格和付款方式。", "屋台で値段と支払い方法を確認してください。")
    val marketDetails = T("시장 메뉴 보기", "Market details", "市场详情", "市場の詳細")
    val bookOnPhone = T("휴대폰으로 식당 예약하기", "Book a table on your phone", "用手机预订餐厅", "スマホでテーブルを予約")
    val bookOnPhoneSub = T("CATCHTABLE Global 에서 예약 가능 시간을 확인하세요.", "Use CATCHTABLE Global to check what's available.", "用 CATCHTABLE Global 查看可预约时间。", "CATCHTABLE Global で空き状況を確認しましょう。")
    val searchToReserve = T("검색부터 예약까지", "From search to reservation", "从搜索到预约", "検索から予約まで")
    val searchToReserveSub = T("앱을 열고 목적지 근처를 검색한 뒤 날짜, 인원, 시간을 고르세요.", "Open the app, search near your destination, then check the date, party size and available times.", "打开应用，搜索目的地附近，然后选择日期、人数和时间。", "アプリを開いて目的地周辺を検索し、日付・人数・時間を選んでください。")
    val checkConfirm = T("예약 확정은 앱에서 확인하세요.", "Check your booking confirmation in the app.", "请在应用中查看预约确认。", "予約確定はアプリで確認してください。")
}
