import 'package:share_plus/share_plus.dart';

/// share_plus 래퍼 — 초대 링크 OS Share Sheet 호출
class InviteShare {
  const InviteShare._();

  static Future<void> shareInviteUrl(String url, {String? subject}) async {
    await Share.share(
      '주니어 앱에 초대합니다! 아래 링크를 눌러 가입하세요.\n$url',
      subject: subject ?? '주니어 초대 링크',
    );
  }
}
