-- 프로필 이미지 예시 데이터 (회원가입 화면의 프로필 사진 선택 목록)
-- 이미지는 DiceBear 아바타 PNG (외부 URL, 앱의 <Image>에서 바로 표시 가능)
-- 여러 번 실행해도 중복으로 들어가지 않음 (image_url, name이 unique라 on conflict do nothing)
--
-- 삭제: delete from profile_image where image_url like 'https://api.dicebear.com/%';
--       (이 이미지를 고른 유저는 FK가 on delete set null이라 프로필 이미지가 null이 됨)

insert into profile_image (image_url, name) values
    ('https://api.dicebear.com/9.x/thumbs/png?seed=bolt&size=256', '번개'),
    ('https://api.dicebear.com/9.x/thumbs/png?seed=leaf&size=256', '나뭇잎'),
    ('https://api.dicebear.com/9.x/thumbs/png?seed=wave&size=256', '파도'),
    ('https://api.dicebear.com/9.x/thumbs/png?seed=sun&size=256', '햇살'),
    ('https://api.dicebear.com/9.x/thumbs/png?seed=moon&size=256', '달빛'),
    ('https://api.dicebear.com/9.x/thumbs/png?seed=star&size=256', '별')
on conflict do nothing;
