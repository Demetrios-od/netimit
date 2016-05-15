function [MSG,CNT] = netimit(num_abons, betta, movarea, speed,...
  NBS, Rad, covzone, maxiters, pause_duration,...
  calldur, Ncar_, Ncpc_, vis)
% Program for imitatonal modeling of cellular networks
% with dynamic channel distribution.
%
% (C) Bukhan D.Yu. 2011-2013

%clc

disp('Start modeling a cellular network with dynamic channel distribution.')

%------------------------- Initial data ---------------------------

%disp('Setting the initial data...')

% global variables
global abon carrier BS BS_neighbors   % databases
global Ncar Ncpc call_duration_aver call_duration_disp

% Abonents
%num_abons=500;   % quantity of abons
%betta=0.05;      % probability of call appearance at an iteration
               % (if multiplied by call duration, gives load
               % created by single abon, in Erlangs)
xmin=movarea.xmin;        % coordinates of the area where abons
xmax=movarea.xmax;        %   can move, in meters
ymin=movarea.ymin;
ymax=movarea.ymax;
%maxspeed=100;
speed_aver=speed.aver;  % abon's speed, meters per iteration (minute)
speed_disp=speed.disp;  %   100 m/min = 6 km/h

% Base stations
%NBS=25;    % number of BS
%Rad=1000;  % cell radius, meters
lbcX=covzone.lbcX;    % coordinates of the left and bottom corner of the coverage zone
lbcY=covzone.lbcY;

% Other important parameters
%maxiters=1440;        % quantity of iterations in modeling
%pause_duration=1;   % pause between iterations, sec
call_duration_aver=calldur.aver;  % average call duration, iterations (minutes)
call_duration_disp=calldur.disp;  % dispersion of call duration, iterations (minutes)
Ncar=Ncar_;       % number of carriers
Ncpc=Ncpc_;       % number of traffic channels per carrier

% Visualization parameters
showiter=vis.showiter;   % show iteration number
everyiter=vis.everyiter;    % every this iteration will be reported
showmes=vis.showmes;   % show info messages
shownet=vis.shownet;   % visualize the cellular network and abons' moving
showbar=vis.showbar;

%disp('Initial data setted.')

%---------------------- Calculation begins ---------------------------

%-------------------- Initialize the network -------------------------

%disp('Network initialization begins.')

if or (((xmax-xmin) <=0), ((ymax-ymin) <= 0))
    error('Incorrect limits of moving zone.');
end

% initialization of the array of abons
%disp('Forming array of abons...')
for i=1:num_abons
    abon(i).x=xmin + rand*(xmax-xmin); % coordinates
    abon(i).y=ymin + rand*(ymax-ymin);
    abon(i).speed=speed_aver+speed_disp*randn;  % abon's speed
    abon(i).dir=rand*2*pi; % direction - from 0 to 2*pi
    abon(i).activ=0;      % abons are inactive for the first time
    abon(i).bts=0;        % assignation with BS will be defined later
    abon(i).channel=0;    % physical channel used by abon
end

% find quantity of BS for vertical and horizontal directions
%disp('Finding quantity of BS for vertical and horizontal directions...')
ns=ceil(sqrt(NBS));
strs=1:ns;
cols=strs;   % numbering of columns

BS_neighbors=eye(NBS);   % list of neighbor BS - is blank yet
% define coordinates of cell centers (BS locations)
%disp('Defining coordinates of cell centers (BS locations)...')
k=1;
for i=strs
    for j=cols
        BS(k).y=lbcY+i*3*Rad/2;
        BS(k).x=abs(i/2-floor(i/2))*Rad*sqrt(3) + lbcX + j*Rad*sqrt(3);
        BS(k).rad=Rad;
        BS(k).carriers=[];
        % define neighboring BS and save them in the matrix BS_neighbors
        % min guard distanse — three closest BS with smaller numbers
        if (j-1)>=1   % point (i; j-1) — at the left side
            BS_neighbors(k,k-1)=1;
            BS_neighbors(k-1,k)=1;
        end
        if ((i-1)>=1) %&& (mod(k,ns)~=0)   % point (i-1; j) - at the bottom
            %k1=k-ns+1-mod(i-1,2);     % is different for odd and even numbers
            BS_neighbors(k,k-ns)=1;
            BS_neighbors(k-ns,k)=1;
        end
        if (i-1)>=1      % point (i-1; j+/-1) - at the left/right and below
            if mod(i,2)   % if row is odd, take righter cell
                k1=k-ns+(mod(k,ns)~=0);
            else      % if row is even, take lefter cell
                k1=k-ns-(mod(k-1,ns)~=0);
            end
            BS_neighbors(k,k1)=1;
            BS_neighbors(k1,k)=1;
        end
        k=k+1;
        if k>NBS
            break
        end
    end
end

% initialization of the carrier array
%disp('Forming array of carriers...')
for i=1:Ncar
    carrier(i).bts=[];    % BS where the carrier is used
    % abons' IDs who are at the channels
    carrier(i).channels=zeros(1,Ncpc);
end

% initialization of the event counters
%disp('Initializing event counters...')

% common events
MSG.ABON_COME=0;        % abon comes to the service zone
MSG.ABON_LEAVE=0;       % abon leaves the service zone
MSG.ABON_HAND_ACT=0;    % active hand-off
MSG.ABON_HAND_PAS=0;    % passive hand-off
MSG.CALL_REQ_SUC=0;     % successful call request
MSG.CALL_REQ_FAIL=0;    % failed call request
MSG.CALL_REQ_OUT=0;     % call requests outside the service zone
MSG.CALL_REL_NORM=0;    % call ends inside cell
MSG.CALL_REL_HAND=0;    % call ends on hand-off
MSG.CALL_REL_LEAVE=0;   % call ends on leaving the service zone

% internal messages
% MI.CHAN_GRANT_SUC=0;    % channel assigned successfully
% MI.CHAN_GRANT_FAIL=0;   % channel asked, but not assigned
% MI.CHAN_REL=0;          % channel released
% MI.CAR_GRANT_SUC=0;     % carrier assigned successfully
% MI.CAR_GRANT_FAIL=0;    % carrier asked, but not assigned
% MI.CAR_REL=0;           % carrier released

% statistic counters
CNT.CELL_CAR=zeros(1,NBS);     % number of busy carriers in every cell
CNT.CELL_CHAN=zeros(1,NBS);    % number of busy channels in every cell
% CNT.CHAN_BUSY=zeros(1,Ncpc);   % number of busy channels at a carrier
% CNT.CAR_BUSY=zeros(1,Ncar);    % number of busy carriers in the service zone

%disp('Initialization done.')

%------------------ Network initialization finished ---------------------

%-------------------- Iterational process begins ------------------------

%disp('Iterational process begins.')

iter=1;
if showbar
    wh=waitbar(0,'Calculation in progress...');
end

while iter<=maxiters
    if showiter && (mod(iter,everyiter)==0)
        disp(strcat('iter=',int2str(iter)))
    end
    if shownet
        cla
        hold on
        for j=1:NBS
            % draw cells
            drawcell(BS(j).x,BS(j).y,BS(j).rad);
            text(BS(j).x,BS(j).y,num2str(j))
        end
        line([xmin, xmin, xmax, xmax, xmin], [ymin, ymax, ymax, ymin, ymin]);
    end
    for i=1:num_abons
        % find new abon's coordinates in its moving
        speed = abon(i).speed * wf((abon(i).x-xmin)/(xmax-xmin), (abon(i).y-ymin)/(ymax-ymin), iter/maxiters);
          % wf(x,y,iter) - EXTERNAL (!!!!) weight function
          % x=0...1; y=0...1; iter=0...1
        delx=speed*cos(abon(i).dir);
        dely=speed*sin(abon(i).dir);
        abon(i).x=abon(i).x+delx;
        abon(i).y=abon(i).y+dely;

         % if abon appears outside the moving zone, it 'reflests' from
         % the edge of the zone
        if or( (abon(i).x < xmin), (abon(i).x > xmax) )
            abon(i).x = abon(i).x - delx;
            abon(i).dir = pi - abon(i).dir;
        end
        if or( (abon(i).y < ymin), (abon(i).y > ymax) )
            abon(i).y = abon(i).y - dely;
            abon(i).dir = 2*pi - abon(i).dir;
        end

        % define distance from abon to every BS
        for j=1:NBS
            dBS(j) = sqrt((BS(j).x-abon(i).x)^2 + (BS(j).y-abon(i).y)^2);
        end
        % find the closest BS to abon, and save its number to abon's property
        last_bsid=abon(i).bts;
        [min_d,closer_bts]=min(dBS);
        if min_d<=BS(closer_bts).rad
            abon(i).bts=closer_bts;
        else
            abon(i).bts=0;
        end

        % Begin messages processing
        if (abon(i).bts ~= last_bsid) && (last_bsid == 0)
            % abon comes to the service zone
            if showmes
                disp(strcat('Abon',num2str(i),...
                    '_comes to the service zone on the BTS',...
                    num2str(abon(i).bts)))
            end
            MSG.ABON_COME=MSG.ABON_COME+1;
        elseif (abon(i).bts == 0) && (last_bsid ~= 0)
            % abon leaves the service zone
            if showmes
                disp(strcat('Abon',num2str(i),...
                    '_leaves the service zone from the BTS',...
                    num2str(last_bsid)))
            end
            MSG.ABON_LEAVE=MSG.ABON_LEAVE+1;
            if abon(i).activ>0
                % if a connection was when leaving the service zone,
                % it must be terminated immediately
                abon(i).activ=0;
                % SYSTEM MESSAGE: call release
                fcn_call_release(i,last_bsid);
                MSG.CALL_REL_LEAVE=MSG.CALL_REL_LEAVE+1;
                if showmes
                    disp(strcat('Call termination: abon',num2str(i),...
                        '_has a terminated call on the BTS',...
                        num2str(last_bsid)));
                end
            end
        elseif (abon(i).bts ~= last_bsid) && (last_bsid ~= 0)
            % changing BS — the hand-off
            if abon(i).activ>0
                % SYSTEM MESSAGE: active hand-off
                MSG.ABON_HAND_ACT=MSG.ABON_HAND_ACT+1;
                if showmes
                    disp(strcat('Active hand-off: abon',num2str(i),...
                        '_leaves BTS',num2str(last_bsid),...
                        '_and connects to BTS',num2str(abon(i).bts)));
                end
                if ~fcn_handover_act(i,last_bsid)
                    % if channel is not assigned in the new cell,
                    % the call termination happens
                    if showmes
                        disp(strcat('Call termination: abon',num2str(i),...
                            '_has a terminated call on the BTS',...
                            num2str(last_bsid)));
                    end
                    abon(i).activ=0;
                    MSG.CALL_REL_HAND=MSG.CALL_REL_HAND+1;
                end
            else
                % SYSTEM MESSAGE: inactive hand-off
                if showmes
                    disp(strcat('Inactive hand-off: abon',num2str(i),...
                        '_leaves BTS',num2str(last_bsid),...
                        '_and connects to BTS',num2str(abon(i).bts)));
                end
                %fcn_handover_pas(i,last_bsid);
                MSG.ABON_HAND_PAS=MSG.ABON_HAND_PAS+1;
            end
        end

        % active time decreases at every iteration
        if abon(i).activ>0
            abon(i).activ=abon(i).activ-1;
            if abon(i).activ <= 0
                % SYSTEM MESSAGE: call release
                fcn_call_release(i,abon(i).bts);
                MSG.CALL_REL_NORM=MSG.CALL_REL_NORM+1;
                if showmes
                    disp(strcat('Call release: abon',num2str(i),...
                        '_has a released call on the BTS',...
                        num2str(abon(i).bts)));
                end
            end
            sf='ro';
            mfc=[1 0 0];
            mbc=[1 0 0];
        else
            if rand<betta
                if abon(i).bts~=0
                    % SYSTEM MESSAGE: call request
                    if fcn_call_request(i)
                        if showmes
                            disp(strcat('Call request: abon',num2str(i),...
                                '_requests a call on the BTS',...
                                num2str(abon(i).bts)));
                        end
                        abon(i).activ=call_duration_aver+call_duration_disp*randn;
                        MSG.CALL_REQ_SUC=MSG.CALL_REQ_SUC+1;
                    else
                        if showmes
                            disp(strcat('There are no available channels on the BTS',...
                                num2str(abon(i).bts),'. Call request failed'))
                        end
                        MSG.CALL_REQ_FAIL=MSG.CALL_REQ_FAIL+1;
                    end
                else
                    if showmes
                        disp(strcat('Abon',num2str(i),...
                            '_requests a call outside the service zone'))
                    end
                    MSG.CALL_REQ_OUT=MSG.CALL_REQ_OUT+1;
                end
            end
            sf='ko';
            mfc=[0 0 0];
            mbc=[0 0 0];
        end

        if shownet
            % draw a point - abon
            %plot(abon(i).x,abon(i).y,sf,'markerfacecolor',mfc,...
            %      'markerbordercolor',mbc,'markersize',4)
            % 'markerbordercolor' is unknown property in Octave
            plot(abon(i).x,abon(i).y,sf,'markerfacecolor',mfc,'markersize',4)
            %text(abon(i).x,abon(i).y,num2str(abon(i).bts))
        end

    end

    % Count values of the counters

    for i=1:NBS
        % calculate the average quantity of busy carriers in every cell
        CNT.CELL_CAR(i)=(CNT.CELL_CAR(i)*(iter-1)+length(BS(i).carriers))/iter;
        % calculate the average quantity of busy channels in every cell
        nch=0;
        for j=BS(i).carriers
            bnid=find(carrier(j).bts==i);
            nch=nch+length(find(carrier(j).channels(bnid,:)));
        end
        CNT.CELL_CHAN(i)=(CNT.CELL_CHAN(i)*(iter-1)+nch)/iter;
    end

    if shownet
        pause(pause_duration)
    end

    if showbar
        waitbar(iter/maxiters,wh);
    end

    iter=iter+1;
end

if showbar
    delete(wh);
end

%clc
disp(strcat('Simulation is finished successfully for_',...
    num2str(iter-1),'_iterations.'))

% Write data to the file.
% For instructions, see the book, page 57.
f=fopen('res.dat','ab');  % opening a binary file for appending

% writing the initial data
fwrite(f,num_abons,'uint16');   % 2
fwrite(f,NBS,'uint16');         % 2
fwrite(f,call_duration_aver,'uint16');  % 2
fwrite(f,call_duration_disp,'uint16');  % 2
fwrite(f,Ncar,'uint16');        % 2
fwrite(f,Ncpc,'uint16');        % 2
fwrite(f,maxiters,'uint32');    % 4
fwrite(f,betta,'float32');      % 4
fwrite(f,speed_aver,'float32');   % 4
fwrite(f,speed_disp,'float32');   % 4
fwrite(f,[xmin xmax ymin ymax],'float32');   % 4*4=16
fwrite(f,[lbcX lbcY],'float32');             % 4*2=8
fwrite(f,Rad,'float32');        % 4

% writing the result of modeling
fwrite(f,MSG.ABON_COME,'uint32');       % 4
fwrite(f,MSG.ABON_LEAVE,'uint32');      % 4
fwrite(f,MSG.ABON_HAND_ACT,'uint32');   % 4
fwrite(f,MSG.ABON_HAND_PAS,'uint32');   % 4
fwrite(f,MSG.CALL_REQ_SUC,'uint32');    % 4
fwrite(f,MSG.CALL_REQ_FAIL,'uint32');   % 4
fwrite(f,MSG.CALL_REQ_OUT,'uint32');    % 4
fwrite(f,MSG.CALL_REL_NORM,'uint32');   % 4
fwrite(f,MSG.CALL_REL_HAND,'uint32');   % 4
fwrite(f,MSG.CALL_REL_LEAVE,'uint32');  % 4

% One record has 96 bytes - 56 for initial data and 40 for results

fclose(f);
disp('Data has been saved to file.')

end


%-------------------- Messages processing functions ----------------------

function [suc] = fcn_handover_act(abon_id,last_bsid)
% processing the active hand-off
fcn_call_release(abon_id,last_bsid);
suc=fcn_call_request(abon_id);
end


% function [] = fcn_handover_pas(abon_id,last_bsid)
% % processing the inactive hand-off
% % nothing to do
% end


function [] = fcn_call_release(abon_id,bts_id)
% processing the call release
global abon carrier BS   % databases
chan=mod(abon(abon_id).channel,100);
car=fix((abon(abon_id).channel-chan)/100);
bnid=find(carrier(car).bts==bts_id);
carrier(car).channels(bnid,chan)=0;  % delete channel from carrier
abon(abon_id).channel=0;             % delete channel from abon
if isempty(find(carrier(car).channels(bnid,:),1))
    % if the carrier is empty, delete it from BS
    carrier(car).bts(bnid)=[];
    carrier(car).channels(bnid,:)=[];
    cid=find(BS(bts_id).carriers==car,1);
    BS(bts_id).carriers(cid)=[];
end
end


function [suc] = fcn_call_request(abon_id)
% processing call request
global abon carrier Ncpc
bts_id=abon(abon_id).bts;
% check if any carrier is assigned to the BS,
% and find the recommended one to use
[carlist,Avcar]=check_neig_carriers(bts_id);
% carlist — the list of carriers used in the cell
% Avcar — new available carrier for this cell
if isempty(carlist)
    % if the cell has no carriers, we must assign new one
    BS_set_carrier(bts_id,Avcar);
else
    % if carriers are in the cell
    % check if busy channels are at the existing carriers,
    % then find the carrier which has max number of busy channels
    num_cc_max=0;
    Avcar2=Avcar;
    for i=carlist
        bnid=find(carrier(i).bts==bts_id);
        num_cc=length(find(carrier(i).channels(bnid,:)~=0));
        if (num_cc>num_cc_max) && (num_cc~=Ncpc)
            num_cc_max=num_cc;
            Avcar2=i;
        end
    end
    if Avcar2==Avcar
        % it is necessary to set a new carrier to this cell
        BS_set_carrier(bts_id,Avcar2);
    end
    Avcar=Avcar2;
end

% now Avcar - the number of carrier which has max quantity of busy channels,
% or new carrier, or emty array
if ~isempty(Avcar)
    bnid=find(carrier(Avcar).bts==bts_id);
    abch=find(carrier(Avcar).channels(bnid,:)==0,1);  % channel allowed for abon
    % assigning channel to the abon
    carrier(Avcar).channels(bnid,abch)=abon_id;
    abon(abon_id).channel=100*Avcar+abch;
    suc=true;
else
    suc=false;
end
end


%------------------ Additional subprograms ---------------------

function drawcell(x,y,r)
% Drawing one cell

p=sqrt(3)/2;
x=x+r*[0, p, p, 0, -p, -p, 0];
y=y+r*[1, 1/2, -1/2, -1, -1/2, 1/2, 1];

line(x,y)
end


function [owncars,avcar] = check_neig_carriers(bts_id)
% owncars - list of carriers used in the cell, sorted
% avcar - new available carrier for this cell
if bts_id==0
    owncars=[];
    avcar=[];
    return
end
global BS_neighbors Ncar carrier
BSneig=find(BS_neighbors(bts_id,:));   % list of neighbor cells
usedcars=[];
owncars=[];
for i=BSneig
    for j=1:Ncar
        if ~isempty(find(carrier(j).bts == i,1))
            % variable 'carrier(j).bts' can contain an array
            usedcars=[usedcars, j];
            if i==bts_id
                owncars=[owncars, j];
            end
        end
    end
end
usedcars=sort(usedcars);
% searching for available carrier at the BS
allcars=zeros(1,Ncar);   % all carriers
allcars(usedcars)=1;
% Select carrier with the smallest number as the first available carrier
avcar=find(allcars==0,1);
end


function [suc] = BS_set_carrier(bts_id,Avcar)
% installing the new carrier to the cell
global carrier BS Ncpc
if ~isempty(Avcar)
    % add current BS to the list of BS which use this carrier
    carrier(Avcar).bts=[carrier(Avcar).bts, bts_id];
    bnid=find(carrier(Avcar).bts==bts_id);
    carrier(Avcar).channels(bnid,:)=zeros(1,Ncpc);
    BS(bts_id).carriers=[BS(bts_id).carriers, Avcar];
    suc=true;
else
    suc=false;
end
end
