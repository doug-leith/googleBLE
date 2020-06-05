# python 2.7

import json
import sys
import time
import socket
import subprocess

ADB = "/Users/doug/Library/Android/sdk/platform-tools/adb"
PHONE_SERVER_IP = "127.0.0.1" #"192.168.1.191" #"127.0.0.1"
PHONE_SERVER_PORT = 8081

def start_app():
	# make sure phone is awake and exposure notification app is running on phone
	#command = ADB + ' shell input keyevent 26'
	#command = ADB + ' -s HT85G1A05551 forward tcp:8081 tcp:8081\n' # forward localhost:8081 to phone:8081
	command = ADB + ' forward tcp:8081 tcp:8081\n' # forward localhost:8081 to phone:8081
	#command += ADB + ' shell am start -n "com.google.android.apps.exposurenotification.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'
	p = subprocess.Popen(command, shell=True,
											 stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	stdout, stderr = p.communicate()
	print('%s%s'%(stdout,stderr))
	time.sleep(1) # might take a while for phone to wake up

def open_conn():
	# open connection to server on phone
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.connect((PHONE_SERVER_IP, PHONE_SERVER_PORT))
	return s

def read_line(s,endline='\n'):
	line=""
	i=0
	while 1:
		res = s.recv(1)
		if (not res) or (res.decode("ascii") == endline): break
		line += res.decode("ascii")
	return line

def read_line_ok(s, cmd):
	# if response is not 'ok', bail
	resp = read_line(s)
	if (resp != 'ok'):  # something went wrong
		print(cmd + " : " + resp)
		exit(-1)

def send_cmd(s,cmd, readline = True):
	# send a command to the phone
	msg = bytearray()
	msg.extend(cmd.encode("ascii"))
	s.sendall(msg)
	if (readline):
		read_line_ok(s,cmd)

def get_tek():
	send_cmd(s,"GET\n",False)
	return read_line(s) # get the  response


def put_tek(tek,start,dur,token):
	send_cmd(s,"PUT "+tek.lower()+" "+str(start)+" "+str(dur)+" "+token+"\n",False)
	return read_line(s,"#") # get the  response

def putlong_tek(tek,start,dur,token):
	send_cmd(s,"PUTLONG "+tek.lower()+" "+str(start)+" "+str(dur)+" "+token+"\n",False)
	return read_line(s,"#") # get the  response

def putshort_tek(tek,start,dur,token):
	send_cmd(s,"PUTSHORT "+tek.lower()+" "+str(start)+" "+str(dur)+" "+token+"\n",False)
	return read_line(s,"#") # get the  response


start_app()
s=open_conn()
#print(get_tek())

#test
print(putshort_tek("b6c00a137242b73fc4cd64fe3c9f5286",2652314,12,"test120"))

#orientation 5th june
#print(putshort_tek("6363b105163f3c0510163e70b43c4827",2652306,6,"orientation0_g2"))

#rooted phone 5 june gapp 6363b105163f3c0510163e70b43c4827
#rooted phone ble 394450ae7c7c6161c3ad084bbb98edcf 2652264 walking 1m side by side
#rooted phone tesco 1m ble 9f1def47a1e6c47f98c28a66d816f2a6 2652271 1410 (but really 14:19) until 1447
#blue phone 5 june gapp c9456412370fb4349fadf46b9dca11f1
#blue phone tesco 2m ble b112d0dfdca89c5e2d93218a446aa410 2652274 1440.  but receiver phone ble was off uintil 1457

#orange phone 5 june gapp 4f028c9b8b68db8f7f88d8620db5241a
#orange phone ble on table 5874d2748ed7862d1291fd9117528446.  might not have been running
#blue phone ble on table 8993b9ae5724e7b2b05b6f9f2a157a18 2652283

#my phone 5 june gapp 762196e8707d84242556f2996aa61bef

#print(putlong_tek("4f028c9b8b68db8f7f88d8620db5241a",2652283,6,"phone1_ontable_g2"))
#print(putlong_tek("5874d2748ed7862d1291fd9117528446",2652283,6,"phone1_ontable_ble1"))
#print(putlong_tek("c9456412370fb4349fadf46b9dca11f1",2652288,6,"phone2_ontable_g2"))
#print(putlong_tek("73bf015b8d69cf8ee19aaddcde3139ce",2652288,6,"phone2_ontable_ble3"))
#print(putlong_tek("4f028c9b8b68db8f7f88d8620db5241a",2652288,6,"phone1_ontable_g3"))

#print(putlong_tek("6363b105163f3c0510163e70b43c4827",2652264,6,"walking1msidebyside_g2"))
#print(putlong_tek("394450ae7c7c6161c3ad084bbb98edcf",2652264,6,"walking1msidebyside_ble3"))
#print(putlong_tek("762196e8707d84242556f2996aa61bef",2652264,6,"walking1msidebyside_rooted_g4"))

#print(putlong_tek("6363b105163f3c0510163e70b43c4827",2652271,6,"tesco_1m_g1"))
#print(putlong_tek("9f1def47a1e6c47f98c28a66d816f2a6",2652271,6,"tesco_1m_ble1"))
#print(putlong_tek("c9456412370fb4349fadf46b9dca11f1",2652274,6,"tesco_2m_g1"))
#print(putlong_tek( "b112d0dfdca89c5e2d93218a446aa410",2652274,6,"tesco_2m_ble4"))

#print(putshort_tek("1a7abbaa003d08cb04707355fd0dd5f4",2652139,144,"rpi4"))

#orientation tests
#rooted phone gapp 16e7d101f90460944259ebe9f1611c70
#orange phone gapp 38e5f9fcf8a5bef1edf4aec846647e55
#green phone gapp 31c758d232a45deec450be5712ea8994
#myphone gapp 5971cc49583a02da49c9d033935cef1d
#myphone bleapp c8fe45c46f156e9d5b573a261a5be8e7
#starttime = 2652139
#print(putlong_tek("16e7d101f90460944259ebe9f1611c70",starttime,6,"orientation_rooted_g1"))
#print(putlong_tek("38e5f9fcf8a5bef1edf4aec846647e55",starttime,6,"orientation_orange_g1"))
#print(putlong_tek("31c758d232a45deec450be5712ea8994",starttime,6,"orientation_green_g3"))
#print(putlong_tek("5971cc49583a02da49c9d033935cef1d",starttime,6,"orientation_myphone_g2"))
#print(putlong_tek("c8fe45c46f156e9d5b573a261a5be8e7",starttime,6,"orientation_myphone_ble2"))

#non-rooted phone, not mine: 55bcb7e092fbc74931518ff095fb0367 4th june
#print(put_tek("55bcb7e092fbc74931518ff095fb0367",2652113,6,"walking1mbehind_dur60_g8"))
#print(put_tek("9433391266cecc2aab95f3f5b7b404fd",2652113,6,"walking1mbehind_dur60_ble9"))

#print(putlong_tek("55bcb7e092fbc74931518ff095fb0367",2652130 ,6,"walkingsidebyside_dur60_g2"))
#print(putlong_tek("6455674795b0f708bf1ff9ea2a2bb34b",2652130 ,6,"walkingsidebyside_dur60_ble5"))

###########
# 1st June 4pm-8pm phone 1:
# ble app TEK: bb97ffb43662123b7f0b74ecc707d299 start interval: 2651707
# google app TEK: e2b8487eb4c65fd87dff0cd5df36a774, rollingStartIntervalNumber: Mon Jun 01 01:00:00 GMT+01:00 2020

#print(put_tek("4b67655e010da81af268ff28f563294c",2651707,6,"garden1m_phone1_googleapp_dur60min"))
#print(put_tek("4b67655e010da81af268ff28f563294c",2651707,12,"garden1m_phone1_googleapp_dur120min"))
#print(put_tek("4b67655e010da81af268ff28f563294c",2651707,18,"garden1m_phone1_googleapp_dur180min"))
#print(put_tek("85754946cf34ddd416e505dff79fbec4",2651707,6,"garden1m_phone1_bleapp_dur60min"))
#print(put_tek("85754946cf34ddd416e505dff79fbec4",2651707,12,"garden1m_phone1_bleapp_dur120min"))
#print(put_tek("85754946cf34ddd416e505dff79fbec4",2651707,18,"garden1m_phone1_bleapp_dur180min"))

###########
# 1st June 4pm-8pm phone 2:
# ble app TEK: TEK: 85754946cf34ddd416e505dff79fbec4 start interval: 2651707
# google app TEK:  4b67655e010da81af268ff28f563294c, rollingStartIntervalNumber: Mon Jun 01 01:00:00 GMT+01:00 2020,

#print(put_tek("e2b8487eb4c65fd87dff0cd5df36a774",2651707,6,"garden1m_phone2_googleapp_dur60min"))
#print(put_tek("e2b8487eb4c65fd87dff0cd5df36a774",2651707,12,"garden1m_phone2_googleapp_dur120min"))
#print(put_tek("e2b8487eb4c65fd87dff0cd5df36a774",2651707,18,"garden1m_phone2_googleapp_dur180min"))
#print(put_tek("bb97ffb43662123b7f0b74ecc707d299",2651707,6,"garden1m_phone2_bleapp_dur60min"))
#print(put_tek("bb97ffb43662123b7f0b74ecc707d299",2651707,12,"garden1m_phone2_bleapp_dur120min"))
#print(put_tek("bb97ffb43662123b7f0b74ecc707d299",2651707,18,"garden1m_phone2_bleapp_dur180min"))

###########
# 1st June 9.16pm-10.20pm phone 1:
#ble_app_TEK="2ce3c52c826c795e08f863b460b4e4e0"; starttime="2651737"
#ble_app_TEK="3b0f5dd76d473379c1758cec495b1b08"; starttime="2651737"
#google_app_TEK="e2b8487eb4c65fd87dff0cd5df36a774"

###########
# 1st June 9.16pm-10.20pm phone 2:
#ble_app_TEK="e1a9009ba9a8bd60928762b8a7f0eaf4"; starttime="2651737"
#google_app_TEK="4b67655e010da81af268ff28f563294c"

#print(put_tek(ble_app_TEK,starttime,2,"garden1m_take2_phone2_googleapp_dur20min"))
#print(put_tek(ble_app_TEK,starttime,4,"garden1m_take2_phone2_googleapp_dur40min"))
#print(put_tek(ble_app_TEK,starttime,6,"garden1m_take2_phone2_googleapp_dur60min"))
#print(put_tek(google_app_TEK,starttime,2,"garden1m_take2_phone2_bleapp_dur20min"))
#print(put_tek(google_app_TEK,starttime,4,"garden1m_take2_phone2_bleapp_du40min"))
#print(put_tek(google_app_TEK,starttime,6,"garden1m_take2_phone2_bleapp_dur60min"))

###########
# 1st June 07.40am-0840 phone 1:

#ble_app_TEK="13dc09cbb1a842f2c40c56879ad2fc81"; starttime="2651882"

#ble_app_TEK="99d810949bce45a2272d00274bd21a9a"; starttime="2651880"
#google_app_TEK="d72033d026abcf3f8e3f5f69046414f9"

#ble_app_TEK="1a7abbaa003d08cb04707355fd0dd5f4"; starttime="2651883"
#google_app_TEK="c4176cf2780ae1174765046231ae3eac"

#mike_phone_TEK = "c4176cf2780ae1174765046231ae3eac"
#doug_phone_TEK="3dafc97afea1a0bb9385e1563b3f3c28"
#print(put_tek("ce98bc4302aaa86dd3cde1eccc645373",2652027,6,"garden_dur60_0.5m_ble9"))
#print(put_tek("3dafc97afea1a0bb9385e1563b3f3c28",2652027,6,"garden_dur60_0.5m_g9"))

##latest
#ble_app_TEK="75dea15ddd02f3645410bbd10246d382"; starttime="2652004"
#google_app_TEK="9cf4786e3c5d90687d97e366a8e4813f"

#print(put_tek(ble_app_TEK,starttime,6,"tt26_dur60_ble7"))
#print(put_tek(google_app_TEK,starttime,6,"tt26_dur60_g7"))





###########
# 1st June 08.40m-0840 phone 2:

#ble_app_TEK="e1a9009ba9a8bd60928762b8a7f0eaf4"; starttime="2651809"
#google_app_TEK="d72033d026abcf3f8e3f5f69046414f9"

#print(put_tek(google_app_TEK,starttime,2,"indoor1m_take22_phone2_googleapp_dur20min"))
#print(put_tek(google_app_TEK,starttime,4,"indoor1m_take22_phone2_googleapp_du40min"))
#print(put_tek(google_app_TEK,starttime,6,"indoor1m_take22_phone2_googleapp_dur60min"))
#print(put_tek(ble_app_TEK,starttime,2,"indoor1m_take22_phone2_bleapp_dur20min"))
#print(put_tek(ble_app_TEK,starttime,4,"indoor1m_take22_phone2_bleapp_dur40min"))
#print(put_tek(ble_app_TEK,starttime,6,"indoor1m_take22_phon2_bleapp_dur60min"))


#####################
#print(put_tek("4862d68b2f78750656c87e50632331f6",2651670,6,"test"))
#print(put_tek("818e7a5a3b2f9c195625a02e04f5cbd0",2651683,6,"test"))
#print(put_tek("b9a8f4430cc7798b9e40eddd866e0875",2651689,6,"test"))
#print(put_tek("b38be9160c4c08b945eb233433cac54d",2651701,12,"test6.65"))

#print(put_tek("dea863bca1145d881566a52a0c35b865",2651269-6,6,"garden3.5_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651275-6,6,"garden2_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651281-6,6,"garden4_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651288-6,8,"kitchen3.5_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651296-6,7,"kitchen2_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651304-6,6*7,"kitchen4_pink"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6,6,"garden1_pink"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651275-6,6,"garden0.5_pink"))

# querying pink phone from 2 hours before 1m garden test up to start of test.  duration 1 hour (6x10 mins)
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6-12,6,"garden1_pink_2h_before"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6-6,6,"garden1_pink_1h_before"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6-4,6,"garden1_pink_40min_before"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6-2,6,"garden1_pink_20min_before"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6-1,6,"garden1_pink_10min_before"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6,6,"garden1_pink"))

# querying pink phone from when sitting in kitchen overnight at 1m distance and then after bluetooth switched off at 0518
# 05:00 30th may 2020 = 2651352 10min intervals since epoch

#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-48,6,"kitchen1_pink_22:00_daybefore"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-36,6,"kitchen1_pink_23:00_daybefore"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-30,6,"kitchen1_pink_00:00"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-24,6,"kitchen1_pink_01:00"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,6,"kitchen1_pink_02:00"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-16,6,"kitchen1_pink_02:20"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-14,6,"kitchen1_pink_02:40"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-12,6,"kitchen1_pink_03:00"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-10,6,"kitchen1_pink_03:20"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-8,6,"kitchen1_pink_03:40"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-6,6,"kitchen1_pink_04:00"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-5,6,"kitchen1_pink_04:10"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-4,6,"kitchen1_pink_04:20"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-3,6,"kitchen1_pink_04:30"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-2,6,"kitchen1_pink_04:40"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-1,6,"kitchen1_pink_04:50"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352,6,"kitchen1_pink_05:00"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352+1,6,"kitchen1_pink_05:10"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352+2,6,"kitchen1_pink_05:20"))

#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,6,"kitchen1_pink_02:00_dur60"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,4,"kitchen1_pink_02:00_dur40"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,3,"kitchen1_pink_02:00_dur30"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,2,"kitchen1_pink_02:00_dur20"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,1,"kitchen1_pink_02:00_dur10"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-3,4,"kitchen1_pink_04:30_dur40"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-3,2,"kitchen1_pink_04:30_dur20"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-3,1,"kitchen1_pink_04:30_dur10"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,12,"kitchen1_pink_02:00_dur120"))
#print(put_tek("386eaf3f7f6511514d09bd81032c7b75",2651352-18,18,"kitchen1_pink_02:00_dur180"))


# check as TEK expires at end of day
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651352-48,6,"kitchen1_pink_21:00_dur60"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651352-48,10,"kitchen1_pink_21:00_dur100"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651352-48,12,"kitchen1_pink_21:00_dur120"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651352-48,18,"kitchen1_pink_21:00_dur180"))
#print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651352-48,24,"kitchen1_pink_21:00_dur240"))
