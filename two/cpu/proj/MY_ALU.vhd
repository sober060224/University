library ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
 
entity ALU is 
port( A,B:in std_logic_vector(7 downto 0);
	  IR_in :in std_logic_vector(3 downto 0);
	  ALU_out:out std_logic_vector(7 downto 0);
	  C:out std_logic
);
end ALU;
 
architecture behave of ALU is
signal dataA,dataB,dataC : std_logic_vector(8 downto 0);
signal cout:std_logic;
begin 
	dataA <= '0' & A;
	dataB <= '0' & B;
	process(dataA,dataB,dataC,IR_in)
	begin
	if(IR_in="0000") then
		dataC <= dataA + dataB;
	elsif(IR_in="0001") then
		dataC <= dataA - dataB;
	elsif(IR_in="0010") then
		dataC <= dataA and dataB;
	elsif(IR_in="0011") then
		dataC <= dataA or dataB;
	elsif(IR_in="0100") then
		dataC <= not dataA ;
	elsif(IR_in="0101") then
		dataC <= dataA NAND dataB;
	elsif(IR_in="0110") then
		dataC <= dataA NOR dataB;
	elsif(IR_in="0111") then
		dataC <= dataA XOR dataB;
	else dataC <="000000000";
	end if;
    
    if(IR_in = "0000") then 
		cout<= dataC(8);
		else cout<='0';
	end if;
	end process;
ALU_out(7 downto 0) <= dataC(7 downto 0);
C<=cout;
end behave;