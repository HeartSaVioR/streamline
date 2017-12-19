/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React,{Component} from 'react';
import _ from 'lodash';
import {ToggleButtonGroup, ToggleButton,FormGroup} from 'react-bootstrap';

class ComponentLogActions extends Component {

  constructor(props) {
    super(props);
  }

  handleCallBackAction = (type,nodeId,value) => {
    const {refType,samplingChangeFunc,durationChangeFunc,logLevelChangeFunc,componentLevelAction,topologyId} = this.props;
    if(refType === undefined){
      componentLevelAction(type,nodeId,value);
    } else {
      switch(type){
      case 'LOG' : logLevelChangeFunc(value);
        break;
      case 'DURATION' : durationChangeFunc(value);
        break;
      case 'SAMPLE' : samplingChangeFunc(value);
        break;
      default:break;
      }
    }
  }

  redirectLogSearch = () => {
    const {viewModeContextRouter,topologyId,selectedNodeId} = this.props;
    viewModeContextRouter.push({
      pathname : 'logsearch/'+topologyId,
      state : {
        componentId : selectedNodeId
      }
    });
  }

  handleInputChange = (e) => {
    this.handleCallBackAction('SAMPLE',selectedNodeId,Number(e.target.value));
  }

  render() {
    const {logLevelValue,durationValue,samlpingValue,refType,selectedNodeId,allComponentLevelAction,topologyId} = this.props;
    let sampleVal = samlpingValue , logVal = logLevelValue, durationVal = durationValue;
    if(allComponentLevelAction){
      const samplingObj = _.find(allComponentLevelAction.samplings, (sample) => sample.componentId === selectedNodeId);
      sampleVal = refType === undefined && samplingObj !== undefined && samplingObj.enabled ?  samplingObj.duration : samlpingValue;
    }
    return (
      <div  className={`${refType !== "" && refType !== undefined ? '' : 'component-log-actions-container'}`}>
        <div className={`${refType !== "" && refType !== undefined ? '' : 'sampling-buttons'}`}>
          {
            !!refType
            ? [<label key={1}>Log Level</label>,
              <ToggleButtonGroup  key={2} type="radio" name="log-level-options" value={logVal} onChange={this.handleCallBackAction.bind(this,'LOG',selectedNodeId)}>
                <ToggleButton className="log-level-btn left" value="TRACE">TRACE</ToggleButton>
                <ToggleButton className="log-level-btn" value="DEBUG">DEBUG</ToggleButton>
                <ToggleButton className="log-level-btn" value="INFO">INFO</ToggleButton>
                <ToggleButton className="log-level-btn" value="WARN">WARN</ToggleButton>
                <ToggleButton className="log-level-btn right" value="ERROR">ERROR</ToggleButton>
              </ToggleButtonGroup>,<br  key={3}/>,
              <label key={4}>Duration</label>,
              <ToggleButtonGroup  key={5} type="radio" name="duration-options" value={durationVal} onChange={this.handleCallBackAction.bind(this,'DURATION',selectedNodeId)}>
                <ToggleButton className="duration-btn left" value={5}>5s</ToggleButton>
                <ToggleButton className="duration-btn" value={10}>10s</ToggleButton>
                <ToggleButton className="duration-btn" value={15}>15s</ToggleButton>
                <ToggleButton className="duration-btn" value={30}>30s</ToggleButton>
                <ToggleButton className="duration-btn" value={60}>1m</ToggleButton>
                <ToggleButton className="duration-btn" value={600}>10m</ToggleButton>
                <ToggleButton className="duration-btn right" value={3600}>1h</ToggleButton>
              </ToggleButtonGroup>,<br  key={6}/>]
            : null
          }
          <label>Sampling Percentage</label>
          <ToggleButtonGroup type="radio" name="sampling-options" value={sampleVal} onChange={this.handleCallBackAction.bind(this,'SAMPLE',selectedNodeId)}>
          <ToggleButton className="sampling-btn left" value={0}>0</ToggleButton>
          <ToggleButton className="sampling-btn" value={1}>1</ToggleButton>
          <ToggleButton className="sampling-btn" value={5}>5</ToggleButton>
          <ToggleButton className="sampling-btn" value={10}>10</ToggleButton>
          <ToggleButton className="sampling-btn" value={15}>15</ToggleButton>
          <ToggleButton className="sampling-btn" value={20}>20</ToggleButton>
          <ToggleButton className="sampling-btn right" value={30}>30</ToggleButton>
          {
            sampleVal > 0
            ? <ToggleButton className="sampling-btn right" value={'disable'}>Disable</ToggleButton>
            : null
          }
          </ToggleButtonGroup>
          <label>Sampling Custom Value</label>
          <input  ref="customSample" style={{width : '50%'}} onChange={this.handleInputChange.bind(this,selectedNodeId)} placeholder="35 to 100"  className="form-control" name="customSample" type="number" min={35} max={100} />
        </div>
        {
          refType === undefined
          ? <div className="actions-list">
              <div>Actions
                <ul>
                  <li><span className="logsearchLink" onClick={this.redirectLogSearch.bind(this)}>View Logs</span></li>
                  {/*<li><a>View Errors</a></li>*/}
                </ul>
                {/*
                  <span>Download</span>
                  <ul>
                    <li><a>Log File</a></li>
                    <li><a>HeapDump</a></li>
                    <li><a>JStack Output</a></li>
                    <li><a>Jprofile Output</a></li>
                  </ul>
                  <span>Go to Ambari</span>
                  <ul>
                    <li><a>Storm Ops </a></li>
                    <li><a>Log Search </a></li>
                  </ul>
                  */}
              </div>
            </div>
          : null
        }
      </div>
    );
  }
}

export default ComponentLogActions;
