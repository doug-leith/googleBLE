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
	send_cmd(s,"PUT "+tek+" "+str(start)+" "+str(dur)+" "+token+"\n",False)
	return read_line(s,"#") # get the  response

start_app()
s=open_conn()
#print(get_tek())
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651269-6,6,"garden3.5_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651275-6,6,"garden2_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651281-6,6,"garden4_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651288-6,8,"kitchen3.5_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651296-6,7,"kitchen2_pink"))
#print(put_tek("dea863bca1145d881566a52a0c35b865",2651304-6,6*7,"kitchen4_pink"))
print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651269-6,6,"garden1_pink"))
print(put_tek("85c1fcbc8471aab4da2cd412a63c6a71",2651275-6,6,"garden0.5_pink"))
