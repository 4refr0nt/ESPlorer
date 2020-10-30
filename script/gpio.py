import pyb
import time
pin = pyb.Pin(2, pyb.Pin.OUT)
for i in range(4):
    print('LED ON')
    pin.value(0)
    time.sleep(1)
    print('LED OFF')
    pin.value(1)
    time.sleep(1)
    print('iteration done.')
print("All done.")
