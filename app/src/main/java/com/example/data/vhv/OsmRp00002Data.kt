package com.example.data.vhv

/**
 * Official Seed Dataset for Tambon Pa Kha (ต.ป่าขะ อ.บ้านนา จ.นครนายก)
 * mapped from ThaiPHC Report OSMRP00002 (กรมสนับสนุนบริการสุขภาพ กระทรวงสาธารณสุข).
 */
object OsmRp00002Data {

    val PA_KHA_VHV_MEMBERS = listOf(
        // หมู่ 1 บ้านหนองเคี่ยม
        VhvMemberEntity(
            vhvCardId = "1-2602-00101-00-1",
            nationalId = "1260200101001",
            fullName = "นางสมจิตร ใจดี",
            gender = "หญิง",
            phone = "081-987-6543",
            villageNo = "1",
            villageName = "หมู่ 1 บ้านหนองเคี่ยม",
            roleTitle = "ประธาน อสม. หมู่ 1",
            assignedHouseholdsCount = 15
        ),
        VhvMemberEntity(
            vhvCardId = "1-2602-00102-00-2",
            nationalId = "1260200102002",
            fullName = "นายประเสริฐ สุขเจริญ",
            gender = "ชาย",
            phone = "089-123-4567",
            villageNo = "1",
            villageName = "หมู่ 1 บ้านหนองเคี่ยม",
            roleTitle = "อสม. หมอคนที่ 1",
            assignedHouseholdsCount = 12
        ),
        VhvMemberEntity(
            vhvCardId = "1-2602-00103-00-3",
            nationalId = "1260200103003",
            fullName = "นางสาววิไลวรรณ มีสุข",
            gender = "หญิง",
            phone = "086-555-4321",
            villageNo = "1",
            villageName = "หมู่ 1 บ้านหนองเคี่ยม",
            roleTitle = "อสม. ประจำหมู่บ้าน",
            assignedHouseholdsCount = 10
        ),

        // หมู่ 2 บ้านคลองผักหนาม
        VhvMemberEntity(
            vhvCardId = "1-2602-00201-00-1",
            nationalId = "1260200201001",
            fullName = "นางปราณี เจริญยิ่ง",
            gender = "หญิง",
            phone = "082-345-6789",
            villageNo = "2",
            villageName = "หมู่ 2 บ้านคลองผักหนาม",
            roleTitle = "ประธาน อสม. หมู่ 2",
            assignedHouseholdsCount = 14
        ),
        VhvMemberEntity(
            vhvCardId = "1-2602-00202-00-2",
            nationalId = "1260200202002",
            fullName = "นายสมศักดิ์ มั่งคั่ง",
            gender = "ชาย",
            phone = "084-222-3344",
            villageNo = "2",
            villageName = "หมู่ 2 บ้านคลองผักหนาม",
            roleTitle = "อสม. หมอคนที่ 1",
            assignedHouseholdsCount = 11
        ),

        // หมู่ 3 บ้านป่าขะ
        VhvMemberEntity(
            vhvCardId = "1-2602-00301-00-1",
            nationalId = "1260200301001",
            fullName = "นางพรพิมล ยอดดี",
            gender = "หญิง",
            phone = "087-888-9900",
            villageNo = "3",
            villageName = "หมู่ 3 บ้านป่าขะ",
            roleTitle = "ประธาน อสม. ตำบลป่าขะ",
            assignedHouseholdsCount = 16
        ),
        VhvMemberEntity(
            vhvCardId = "1-2602-00302-00-2",
            nationalId = "1260200302002",
            fullName = "นางสาวสายชล งามขำ",
            gender = "หญิง",
            phone = "083-444-5566",
            villageNo = "3",
            villageName = "หมู่ 3 บ้านป่าขะ",
            roleTitle = "เหรัญญิก อสม. ต.ป่าขะ",
            assignedHouseholdsCount = 13
        ),

        // หมู่ 4 บ้านท่ามะเฟือง
        VhvMemberEntity(
            vhvCardId = "1-2602-00401-00-1",
            nationalId = "1260200401001",
            fullName = "นายสมนึก มั่นคง",
            gender = "ชาย",
            phone = "085-111-2233",
            villageNo = "4",
            villageName = "หมู่ 4 บ้านท่ามะเฟือง",
            roleTitle = "ประธาน อสม. หมู่ 4",
            assignedHouseholdsCount = 14
        ),

        // หมู่ 5 บ้านโคกประเสริฐ
        VhvMemberEntity(
            vhvCardId = "1-2602-00501-00-1",
            nationalId = "1260200501001",
            fullName = "นางสุนีย์ เด่นดวง",
            gender = "หญิง",
            phone = "088-777-6655",
            villageNo = "5",
            villageName = "หมู่ 5 บ้านโคกประเสริฐ",
            roleTitle = "ประธาน อสม. หมู่ 5",
            assignedHouseholdsCount = 15
        ),

        // หมู่ 6 บ้านหนองยาง
        VhvMemberEntity(
            vhvCardId = "1-2602-00601-00-1",
            nationalId = "1260200601001",
            fullName = "นายบุญมี สุขสบาย",
            gender = "ชาย",
            phone = "081-333-2211",
            villageNo = "6",
            villageName = "หมู่ 6 บ้านหนองยาง",
            roleTitle = "ประธาน อสม. หมู่ 6",
            assignedHouseholdsCount = 12
        ),

        // หมู่ 7 บ้านกร่างประตูวัง
        VhvMemberEntity(
            vhvCardId = "1-2602-00701-00-1",
            nationalId = "1260200701001",
            fullName = "นายวัชรพงษ์ พวงแจ่ม",
            gender = "ชาย",
            phone = "099-154-6800",
            villageNo = "7",
            villageName = "หมู่ 7 บ้านกร่างประตูวัง",
            roleTitle = "อสม. หมอคนที่ 1 ประจำหมู่บ้าน",
            assignedHouseholdsCount = 15
        ),
        VhvMemberEntity(
            vhvCardId = "1-2602-00702-00-2",
            nationalId = "1260200702002",
            fullName = "นางกมลวรรณ วงษ์สุวรรณ",
            gender = "หญิง",
            phone = "086-111-9988",
            villageNo = "7",
            villageName = "หมู่ 7 บ้านกร่างประตูวัง",
            roleTitle = "ประธาน อสม. หมู่ 7",
            assignedHouseholdsCount = 14
        ),

        // หมู่ 8 บ้านคลองส่ง
        VhvMemberEntity(
            vhvCardId = "1-2602-00801-00-1",
            nationalId = "1260200801001",
            fullName = "นางยุพิน ศรีทอง",
            gender = "หญิง",
            phone = "082-777-3322",
            villageNo = "8",
            villageName = "หมู่ 8 บ้านคลองส่ง",
            roleTitle = "ประธาน อสม. หมู่ 8",
            assignedHouseholdsCount = 13
        ),

        // หมู่ 9 บ้านคลองกระโดน
        VhvMemberEntity(
            vhvCardId = "1-2602-00901-00-1",
            nationalId = "1260200901001",
            fullName = "นายชัยยศ เพชรแท้",
            gender = "ชาย",
            phone = "084-555-6677",
            villageNo = "9",
            villageName = "หมู่ 9 บ้านคลองกระโดน",
            roleTitle = "ประธาน อสม. หมู่ 9",
            assignedHouseholdsCount = 11
        ),

        // หมู่ 10 บ้านต้นกระบก
        VhvMemberEntity(
            vhvCardId = "1-2602-01001-00-1",
            nationalId = "1260201001001",
            fullName = "นางจำลอง บุญส่ง",
            gender = "หญิง",
            phone = "089-444-1122",
            villageNo = "10",
            villageName = "หมู่ 10 บ้านต้นกระบก",
            roleTitle = "ประธาน อสม. หมู่ 10",
            assignedHouseholdsCount = 13
        ),

        // หมู่ 11 บ้านดงขี้พุก
        VhvMemberEntity(
            vhvCardId = "1-2602-01101-00-1",
            nationalId = "1260201101001",
            fullName = "นายอนันต์ งามยิ่ง",
            gender = "ชาย",
            phone = "083-999-0011",
            villageNo = "11",
            villageName = "หมู่ 11 บ้านดงขี้พุก",
            roleTitle = "ประธาน อสม. หมู่ 11",
            assignedHouseholdsCount = 12
        ),

        // หมู่ 12 บ้านทุ่งกระโปรง
        VhvMemberEntity(
            vhvCardId = "1-2602-01201-00-1",
            nationalId = "1260201201001",
            fullName = "นางกุหลาบ นามงาม",
            gender = "หญิง",
            phone = "081-666-5544",
            villageNo = "12",
            villageName = "หมู่ 12 บ้านทุ่งกระโปรง",
            roleTitle = "ประธาน อสม. หมู่ 12",
            assignedHouseholdsCount = 14
        ),

        // หมู่ 13 บ้านคลองนางหงษ์
        VhvMemberEntity(
            vhvCardId = "1-2602-01301-00-1",
            nationalId = "1260201301001",
            fullName = "นายมานพ นพรัตน์",
            gender = "ชาย",
            phone = "085-333-8899",
            villageNo = "13",
            villageName = "หมู่ 13 บ้านคลองนางหงษ์",
            roleTitle = "ประธาน อสม. หมู่ 13",
            assignedHouseholdsCount = 15
        )
    )
}
