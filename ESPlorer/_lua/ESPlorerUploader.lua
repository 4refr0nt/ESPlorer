if speed == nil then
     speed=9600;
end
uart.setup( 0, speed, 8, 0, 1, 0 );
isOpen=0;
function FileReceive (data)
--     tmr.wdclr();
     if string.match(data,"^espcmd-file-open") then
          fn = string.sub(data,14);
          file.remove(fn);
          file.open(fn,"w+");
          isOpen=1;
     elseif string.match(data,"^espcmd-file-close") then
          fn=nil;
          file.close();
          uart.on("data") ;
          uart.setup( 0, speed, 8, 0, 1, 1 );
          isOpen=0;
          data=nil;
          collectgarbage();
     elseif isOpen==1 then
          file.writeline(data);
     else
          -- file not open - do nothing
     end
end
uart.on("data",FileReceive, 0);
